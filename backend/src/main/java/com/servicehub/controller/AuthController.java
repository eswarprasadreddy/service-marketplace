package com.servicehub.controller;

import com.servicehub.config.JwtService;
import com.servicehub.error.ApiException;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository users, JwtService jwtService) { this.users = users; this.jwtService = jwtService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public synchronized Map<String, Object> register(@RequestBody Map<String, Object> body) {
        String name = text(body, "name"); String email = text(body, "email");
        String password = text(body, "password"); String role = text(body, "role");
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || !role.matches("CUSTOMER|WORKER|ADMIN")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name, email, password, and a valid role are required.");
        }
        if ("ADMIN".equals(role) && users.countByRole("ADMIN") > 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "An admin account already exists. Ask an admin to create another one.");
        }
        if (password.length() < 6) throw new ApiException(HttpStatus.BAD_REQUEST, "Password must have at least 6 characters.");
        User user = new User(); user.setName(name.trim()); user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(password)); user.setRole(role);
        user = users.save(user);
        return authResponse(user);
    }

    @GetMapping("/admin-setup")
    public Map<String, Boolean> adminSetup() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("available", users.countByRole("ADMIN") == 0);
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String email = text(body, "email").trim().toLowerCase(Locale.ROOT);
        String password = text(body, "password");
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }
        return authResponse(user);
    }

    private Map<String, Object> authResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", jwtService.createToken(user)); response.put("user", ApiData.user(user));
        return response;
    }

    private String text(Map<String, Object> body, String field) {
        Object value = body.get(field);
        return value == null ? "" : String.valueOf(value);
    }
}
