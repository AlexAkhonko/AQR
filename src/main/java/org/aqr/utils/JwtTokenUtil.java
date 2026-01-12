package org.aqr.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.aqr.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenUtil {

    /**
     * Вариант A (рекомендуется): хранить секрет в Base64 (длинный случайный).
     * Тогда используйте Decoders.BASE64.decode(secret)
     */
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}") // 24 часа по умолчанию
    private long expirationMs;

    /**
     * Генерация токена для пользователя
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getLogin())
                .issuedAt(now)
                .expiration(exp)
                // можно добавлять кастомные клеймы:
                .claims(Map.of("userId", user.getId()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Генерация токена с кастомными claims (если понадобится)
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .claims(claims)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Достаём username (subject) из токена
     */
    public String getUsernameFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    /**
     * Валидация токена (без сравнения с пользователем)
     */
    public boolean validateToken(String token) {
        try {
            // главное: parseSignedClaims + verifyWith(key)
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            //log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Валидация токена + сравнение subject с текущим пользователем
     * (удобно для JwtRequestFilter)
     */
    public boolean validateToken(String token, User user) {
        try {
            Claims claims = getAllClaims(token);
            String subject = claims.getSubject();
            Date exp = claims.getExpiration();

            return subject != null
                    && subject.equals(user.getLogin())
                    && exp != null
                    && exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            //log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Возвращает все claims (payload)
     */
    public Claims getAllClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);

        return jws.getPayload();
    }

    /**
     * Ключ для подписи/проверки.
     *
     * Если секрет хранится как Base64:
     *  - secret = "b64...."
     *  - byte[] keyBytes = Decoders.BASE64.decode(secret);
     *
     * Если секрет хранится как обычная строка:
     *  - byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
     */
    private SecretKey getSigningKey() {
        // Вариант A: Base64 секрет (предпочтительнее)
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // Вариант B: секрет обычной строкой (если не Base64)
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }
}
