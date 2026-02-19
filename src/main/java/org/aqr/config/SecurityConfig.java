package org.aqr.config;

import lombok.RequiredArgsConstructor;
import org.aqr.service.UserService;
import org.aqr.utils.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final UserService userService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            org.aqr.entity.User user = userService.findByLogin(username);  // ✅ Ваша реализация
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getLogin())
                    .password(user.getPassword())
                    .authorities("ROLE_USER")
                    .build();
        };
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider() {  // ✅ Без параметров!
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());  // ✅ setUserDetailsService
        provider.setPasswordEncoder(passwordEncoder());        // ✅ setPasswordEncoder
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 1) API цепочка: только /api/**, JWT, stateless, CSRF выключен.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {  // ✅ Без параметров!
        return http
                .securityMatcher("/api/**")  // ✅ ДОБАВЬ ЭТО ПЕРВЫМ!
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(daoAuthProvider())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 2) WEB цепочка: всё остальное, formLogin, сессии, CSRF включён.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/**")
                .authenticationProvider(daoAuthProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/dashboard").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/scan", "/images/**").permitAll()  // ✅ Камера + картинки
                        .requestMatchers("/media/**").authenticated()
                        .requestMatchers("/containers/**").authenticated()
                        .requestMatchers("/files/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                )
                // CSRF НЕ отключаем для web: формы Thymeleaf будут отправлять _csrf.
                .build();



    }
}
