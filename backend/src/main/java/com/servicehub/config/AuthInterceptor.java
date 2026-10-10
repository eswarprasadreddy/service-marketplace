package com.servicehub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Browsers send OPTIONS before cross-origin JSON requests such as registration.
        // Let Spring's CORS configuration validate those preflight requests before auth.
        if (request.getMethod().equals(HttpMethod.OPTIONS.name()) || isPublic(request)) return true;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(response, "Sign in first.");
        }
        try {
            Claims claims = jwtService.readToken(header.substring(7));
            request.setAttribute("userId", claims.get("id", String.class));
            request.setAttribute("userRole", claims.get("role", String.class));
            return true;
        } catch (Exception error) {
            return unauthorized(response, "Token is invalid or expired.");
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/health")
                || (path.startsWith("/api/auth/") && (request.getMethod().equals("POST")))
                || (path.equals("/api/auth/admin-setup") && request.getMethod().equals(HttpMethod.GET.name()))
                || (path.equals("/api/services") && request.getMethod().equals(HttpMethod.GET.name()))
                || (path.startsWith("/api/reviews/worker/") && request.getMethod().equals(HttpMethod.GET.name()));
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
        return false;
    }
}
