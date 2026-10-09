package com.servicehub.repository;

import com.servicehub.model.Bid;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, String> {
    List<Bid> findByRequestIdOrderByPriceAsc(String requestId);
    List<Bid> findByWorkerIdAndStatusOrderByCreatedAtDesc(String workerId, String status);
    Optional<Bid> findFirstByRequestIdAndWorkerIdAndStatus(String requestId, String workerId, String status);
    List<Bid> findByRequestId(String requestId);
}
