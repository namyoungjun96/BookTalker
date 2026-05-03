package com.example.book_talker_backend.review;

import com.example.book_talker_backend.review.entity.dto.ReviewDetailResponse;
import com.example.book_talker_backend.review.entity.dto.ReviewListResponse;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.book_talker_backend.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<Void> insertReview(
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        reviewService.insertReview(request, email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/next-reading")
    public ResponseEntity<Void> insertNextReading(
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        reviewService.insertNextReading(request, email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<Void> updateReview(
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        reviewService.updateReview(request, email);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @DeleteMapping
    public ResponseEntity<Void> deleteReview(
            @RequestParam Long reviewId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        reviewService.deleteReview(reviewId, email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReviewListResponse>> getUserReviews(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        return new ResponseEntity<>(reviewService.getReviews(email), HttpStatus.OK);
    }

    @GetMapping("/detail")
    public ResponseEntity<ReviewDetailResponse> getReviewDetail(
            @RequestParam Long reviewId,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String email = (String) response.get("email");
        return new ResponseEntity<>(reviewService.getReviewDetail(reviewId, email), HttpStatus.OK);
    }

}