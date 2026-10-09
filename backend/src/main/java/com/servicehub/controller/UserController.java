package com.servicehub.controller;

import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    public UserController(UserRepository users) { this.users = users; }

    @GetMapping
    public List<Map<String, Object>> all(HttpServletRequest request) {
        ServiceController.requireRole(request, "ADMIN");
        return users.findAll().stream().map(ApiData::user).collect(Collectors.toList());
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAdmin(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        ServiceController.requireRole(request, "ADMIN");
        String name = text(body, "name");
        String email = text(body, "email");
        String password = text(body, "password");
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            throw new com.servicehub.error.ApiException(HttpStatus.BAD_REQUEST, "Name, email, and password are required.");
        }
        if (password.length() < 6) {
            throw new com.servicehub.error.ApiException(HttpStatus.BAD_REQUEST, "Password must have at least 6 characters.");
        }
        User admin = new User();
        admin.setName(name.trim());
        admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
        admin.setPassword(new BCryptPasswordEncoder().encode(password));
        admin.setRole("ADMIN");
        return ApiData.user(users.save(admin));
    }

    private String text(Map<String, Object> body, String field) {
        Object value = body.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
