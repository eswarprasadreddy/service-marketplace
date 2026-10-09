package com.servicehub.controller;

import com.servicehub.error.ApiException;
import com.servicehub.model.ServiceType;
import com.servicehub.repository.ServiceRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ServiceRepository services;
    public ServiceController(ServiceRepository services) { this.services = services; }

    @GetMapping
    public List<Map<String, Object>> getServices() {
        return services.findAllByOrderByNameAsc().stream().map(ApiData::service).collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createService(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireRole(request, "ADMIN");
        String name = text(body, "name").trim(); String description = text(body, "description").trim();
        if (name.isEmpty() || description.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Name and description are required.");
        ServiceType service = new ServiceType(); service.setName(name); service.setDescription(description);
        return ApiData.service(services.save(service));
    }

    static void requireRole(HttpServletRequest request, String role) {
        if (!role.equals(request.getAttribute("userRole"))) throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission.");
    }

    static String userId(HttpServletRequest request) { return String.valueOf(request.getAttribute("userId")); }
    static String text(Map<String, Object> body, String key) { Object value = body.get(key); return value == null ? "" : String.valueOf(value); }
}
