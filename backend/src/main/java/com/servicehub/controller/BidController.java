package com.servicehub.controller;

import com.servicehub.error.ApiException;
import com.servicehub.model.Bid;
import com.servicehub.model.ServiceRequest;
import com.servicehub.repository.BidRepository;
import com.servicehub.repository.ServiceRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/bids")
public class BidController {
    private final BidRepository bids;
    private final ServiceRequestRepository requests;
    private final ServiceRepository services;
    private final UserRepository users;
    public BidController(BidRepository bids, ServiceRequestRepository requests, ServiceRepository services, UserRepository users) {
        this.bids = bids; this.requests = requests; this.services = services; this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        ServiceController.requireRole(request, "WORKER");
        String requestId = ServiceController.text(body, "request").trim();
        String message = ServiceController.text(body, "message").trim();
        double price;
        try { price = Double.parseDouble(ServiceController.text(body, "price")); }
        catch (NumberFormatException error) { price = 0; }
        if (requestId.isEmpty() || price <= 0 || message.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request, a positive price, and message are required.");
        }
        ServiceRequest serviceRequest = requests.findById(requestId).orElse(null);
        if (serviceRequest == null || !"OPEN".equals(serviceRequest.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Open request not found.");
        }
        Bid bid = new Bid(); bid.setRequestId(requestId); bid.setWorkerId(ServiceController.userId(request));
        bid.setPrice(price); bid.setMessage(message);
        return ApiData.bid(bids.save(bid), users);
    }

    @GetMapping("/request/{requestId}")
    public List<Map<String, Object>> getForRequest(@PathVariable String requestId, HttpServletRequest request) {
        ServiceRequest item = requests.findById(requestId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found."));
        String role = String.valueOf(request.getAttribute("userRole"));
        if (!item.getCustomerId().equals(ServiceController.userId(request)) && !"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the customer can view these bids.");
        }
        return bids.findByRequestIdOrderByPriceAsc(requestId).stream().map(bid -> ApiData.bid(bid, users)).collect(Collectors.toList());
    }

    @PutMapping("/{id}/select")
    @Transactional
    public Map<String, Object> select(@PathVariable String id, HttpServletRequest request) {
        ServiceController.requireRole(request, "CUSTOMER");
        Bid selected = bids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bid not found."));
        ServiceRequest item = requests.findById(selected.getRequestId()).orElse(null);
        if (item == null || !item.getCustomerId().equals(ServiceController.userId(request))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the customer who made this request can select a bid.");
        }
        if (!"OPEN".equals(item.getStatus()) || !"PENDING".equals(selected.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This request or bid has already been selected.");
        }
        List<Bid> allBids = bids.findByRequestId(item.getId());
        for (Bid bid : allBids) bid.setStatus(bid.getId().equals(selected.getId()) ? "SELECTED" : "REJECTED");
        item.setStatus("SELECTED");
        bids.saveAll(allBids); requests.save(item);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> selectedData = new HashMap<>();
        selectedData.put("_id", selected.getId()); selectedData.put("request", selected.getRequestId());
        selectedData.put("worker", selected.getWorkerId()); selectedData.put("price", selected.getPrice());
        selectedData.put("message", selected.getMessage()); selectedData.put("status", "SELECTED");
        Map<String, Object> requestData = new HashMap<>(); requestData.put("_id", item.getId()); requestData.put("status", "SELECTED");
        result.put("message", "Bid selected."); result.put("bid", selectedData); result.put("request", requestData);
        return result;
    }
}
