package com.occupify.identity.repository;

import com.occupify.identity.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByContractId(UUID contractId);
    List<Review> findByRevieweeId(UUID revieweeId);
    List<Review> findByReviewerId(UUID reviewerId);
}
