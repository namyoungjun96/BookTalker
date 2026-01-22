package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public int insertReview(ReviewRequest request) {
        Review review = request.to();

        try {
            reviewRepository.save(review);
        } catch (IllegalArgumentException | OptimisticLockingFailureException e) {
            return 0;
        }

        return 1;
    }
}
