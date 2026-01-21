package com.example.book_talker_backend.review;

import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import com.example.book_talker_backend.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public void insertReview(ReviewRequest request) {
        reviewService.insertReview(request);
    }
}