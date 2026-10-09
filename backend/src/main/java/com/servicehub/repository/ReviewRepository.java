package com.servicehub.repository;

import com.servicehub.model.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByWorkerIdOrderByCreatedAtDesc(String workerId);
}
