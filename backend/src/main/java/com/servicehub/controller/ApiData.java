package com.servicehub.controller;

import com.servicehub.model.Bid;
import com.servicehub.model.Review;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.ServiceType;
import com.servicehub.model.User;
import com.servicehub.repository.ServiceRepository;
import com.servicehub.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;

final class ApiData {
    private ApiData() { }

    static Map<String, Object> user(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId()); data.put("name", user.getName()); data.put("email", user.getEmail()); data.put("role", user.getRole());
        return data;
    }

    static Map<String, Object> service(ServiceType service) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_id", service.getId()); data.put("name", service.getName()); data.put("description", service.getDescription());
        return data;
    }

    static Map<String, Object> request(ServiceRequest request, ServiceRepository services, UserRepository users) {
        ServiceType service = services.findById(request.getServiceId()).orElse(null);
        User customer = users.findById(request.getCustomerId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_id", request.getId()); data.put("description", request.getDescription()); data.put("location", request.getLocation());
        data.put("status", request.getStatus()); data.put("createdAt", request.getCreatedAt());
        data.put("service", service == null ? null : service(service));
        Map<String, Object> customerData = new LinkedHashMap<>();
        customerData.put("_id", request.getCustomerId()); customerData.put("name", customer == null ? "Customer" : customer.getName());
        data.put("customer", customerData);
        return data;
    }

    static Map<String, Object> bid(Bid bid, UserRepository users) {
        User worker = users.findById(bid.getWorkerId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_id", bid.getId()); data.put("request", bid.getRequestId()); data.put("price", bid.getPrice());
        data.put("message", bid.getMessage()); data.put("status", bid.getStatus());
        Map<String, Object> workerData = new LinkedHashMap<>();
        workerData.put("_id", bid.getWorkerId()); workerData.put("name", worker == null ? "Service worker" : worker.getName());
        data.put("worker", workerData);
        return data;
    }

    static Map<String, Object> job(Bid bid, ServiceRequest request, ServiceRepository services, UserRepository users) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_id", bid.getId()); data.put("price", bid.getPrice()); data.put("message", bid.getMessage()); data.put("status", bid.getStatus());
        data.put("request", request(request, services, users));
        return data;
    }

    static Map<String, Object> review(Review review, ServiceRequest request, UserRepository users) {
        User customer = users.findById(review.getCustomerId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_id", review.getId()); data.put("rating", review.getRating()); data.put("comment", review.getComment()); data.put("createdAt", review.getCreatedAt());
        Map<String, Object> customerData = new LinkedHashMap<>();
        customerData.put("_id", review.getCustomerId()); customerData.put("name", customer == null ? "Customer" : customer.getName());
        data.put("customer", customerData);
        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("_id", request.getId()); requestData.put("description", request.getDescription());
        data.put("request", requestData);
        return data;
    }
}
