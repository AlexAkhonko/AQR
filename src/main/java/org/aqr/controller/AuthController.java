package org.aqr.controller;

import jakarta.validation.Valid;
import org.aqr.dto.AuthRequest;
import org.aqr.dto.AuthResponse;
import org.aqr.dto.RegisterRequest;
import org.aqr.entity.User;
import org.aqr.service.UserService;
import org.aqr.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired private JwtTokenUtil jwtTokenUtil;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest request) {
        authenticate(request.getLogin(), request.getPassword());
        User user = userService.findByLogin(request.getLogin());
        String jwt = jwtTokenUtil.generateToken(user);
        AuthResponse response = new AuthResponse(jwt);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody @Valid RegisterRequest request) {

        if (userService.existsByLogin(request.getLogin())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Логин уже занят"));
        }

        User user = new User();
        user.setLogin(request.getLogin());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userService.save(user);

        return ResponseEntity.ok(Map.of("message", "Пользователь создан"));
    }

    private void authenticate(String login, String password) {
        try{
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(login, password));
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }

        //SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

