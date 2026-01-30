package com.example.book_talker_backend.review;

import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.book_talker_backend.review.service.ReviewService;
import com.example.book_talker_backend.review.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Void> insertReview(@RequestBody ReviewRequest request) {
        reviewService.insertReview(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<Void> updateReview(@RequestBody ReviewRequest request) {
        reviewService.updateReview(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping("/list")
    public ResponseEntity<List<Review>> getUserReviews(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        List<Review> reviews = reviewService.getReviews(email);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
}