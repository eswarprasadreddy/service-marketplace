package com.servicehub.controller;

import com.servicehub.error.ApiException;
import com.servicehub.model.Bid;
import com.servicehub.model.ServiceRequest;
import com.servicehub.repository.BidRepository;
import com.servicehub.repository.ServiceRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final ServiceRequestRepository requests;
    private final ServiceRepository services;
    private final UserRepository users;
    private final BidRepository bids;

    public RequestController(ServiceRequestRepository requests, ServiceRepository services, UserRepository users, BidRepository bids) {
        this.requests = requests; this.services = services; this.users = users; this.bids = bids;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest httpRequest) {
        ServiceController.requireRole(httpRequest, "CUSTOMER");
        String serviceId = ServiceController.text(body, "service").trim();
        String description = ServiceController.text(body, "description").trim();
        String location = ServiceController.text(body, "location").trim();
        if (serviceId.isEmpty() || description.isEmpty() || location.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Service, description, and location are required.");
        }
        if (!services.existsById(serviceId)) throw new ApiException(HttpStatus.NOT_FOUND, "Service not found.");
        ServiceRequest item = new ServiceRequest(); item.setCustomerId(ServiceController.userId(httpRequest));
        item.setServiceId(serviceId); item.setDescription(description); item.setLocation(location);
        return ApiData.request(requests.save(item), services, users);
    }

    @GetMapping
    public List<Map<String, Object>> getAll(HttpServletRequest request) {
        String role = String.valueOf(request.getAttribute("userRole"));
        String userId = ServiceController.userId(request);
        List<ServiceRequest> found;
        if ("CUSTOMER".equals(role)) found = requests.findByCustomerIdOrderByCreatedAtDesc(userId);
        else if ("WORKER".equals(role)) found = requests.findByStatusOrderByCreatedAtDesc("OPEN");
        else found = requests.findAllByOrderByCreatedAtDesc();
        return found.stream().map(item -> ApiData.request(item, services, users)).collect(Collectors.toList());
    }

    @GetMapping("/my-jobs")
    public List<Map<String, Object>> myJobs(HttpServletRequest request) {
        ServiceController.requireRole(request, "WORKER");
        List<Bid> selected = bids.findByWorkerIdAndStatusOrderByCreatedAtDesc(ServiceController.userId(request), "SELECTED");
        return selected.stream().map(bid -> requests.findById(bid.getRequestId())
                .map(item -> ApiData.job(bid, item, services, users)).orElse(null))
                .filter(item -> item != null).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable String id, HttpServletRequest request) {
        ServiceRequest item = requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found."));
        String userId = ServiceController.userId(request);
        String role = String.valueOf(request.getAttribute("userRole"));
        boolean allowed = ("CUSTOMER".equals(role) && item.getCustomerId().equals(userId)) || "ADMIN".equals(role);
        if ("WORKER".equals(role)) {
            allowed = "OPEN".equals(item.getStatus()) || bids.findFirstByRequestIdAndWorkerIdAndStatus(item.getId(), userId, "SELECTED").isPresent();
        }
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view this request.");
        return ApiData.request(item, services, users);
    }
}
