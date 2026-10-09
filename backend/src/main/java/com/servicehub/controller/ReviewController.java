package com.servicehub.controller;

import com.servicehub.error.ApiException;
import com.servicehub.model.Bid;
import com.servicehub.model.Review;
import com.servicehub.model.ServiceRequest;
import com.servicehub.repository.BidRepository;
import com.servicehub.repository.ReviewRepository;
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
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewRepository reviews;
    private final ServiceRequestRepository requests;
    private final BidRepository bids;
    private final UserRepository users;
    public ReviewController(ReviewRepository reviews, ServiceRequestRepository requests, BidRepository bids, UserRepository users) {
        this.reviews = reviews; this.requests = requests; this.bids = bids; this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        ServiceController.requireRole(request, "CUSTOMER");
        String workerId = ServiceController.text(body, "worker").trim();
        String requestId = ServiceController.text(body, "request").trim();
        String comment = ServiceController.text(body, "comment").trim();
        int rating;
        try { rating = Integer.parseInt(ServiceController.text(body, "rating")); }
        catch (NumberFormatException error) { rating = 0; }
        if (workerId.isEmpty() || requestId.isEmpty() || comment.isEmpty() || rating < 1 || rating > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Worker, request, rating, and comment are required.");
        }
        ServiceRequest item = requests.findById(requestId).orElse(null);
        if (item == null || !item.getCustomerId().equals(ServiceController.userId(request)) || !"SELECTED".equals(item.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can review a worker after selecting their bid.");
        }
        Bid selectedBid = bids.findFirstByRequestIdAndWorkerIdAndStatus(requestId, workerId, "SELECTED").orElse(null);
        if (selectedBid == null) throw new ApiException(HttpStatus.BAD_REQUEST, "This worker was not selected for the request.");
        Review review = new Review(); review.setCustomerId(ServiceController.userId(request)); review.setWorkerId(workerId);
        review.setRequestId(requestId); review.setRating(rating); review.setComment(comment);
        return ApiData.review(reviews.save(review), item, users);
    }

    @GetMapping("/worker/{workerId}")
    public List<Map<String, Object>> byWorker(@PathVariable String workerId) {
        return reviews.findByWorkerIdOrderByCreatedAtDesc(workerId).stream().map(review -> {
            ServiceRequest item = requests.findById(review.getRequestId()).orElse(null);
            return item == null ? null : ApiData.review(review, item, users);
        }).filter(review -> review != null).collect(Collectors.toList());
    }
}
