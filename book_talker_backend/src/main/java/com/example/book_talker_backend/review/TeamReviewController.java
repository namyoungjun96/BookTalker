package com.example.book_talker_backend.review;

import com.example.book_talker_backend.review.entity.dto.TeamReviewDetailResponse;
import com.example.book_talker_backend.review.entity.dto.TeamReviewsResponse;
import com.example.book_talker_backend.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{teamId}/reviews")
@Slf4j
public class TeamReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<TeamReviewsResponse> getTeamReviews(
            @PathVariable Long teamId,
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestParam(required = false, defaultValue = "1") Integer page
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        return new ResponseEntity<>(reviewService.getTeamReviews(teamId, page, providerId), HttpStatus.OK);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<TeamReviewDetailResponse> getTeamReviewDetail(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @PathVariable Long teamId,
            @PathVariable Long reviewId
    ) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        String providerId = (String) response.get("id");

        return new ResponseEntity<>(reviewService.getTeamReviewDetail(teamId, reviewId, providerId), HttpStatus.OK);
    }
}
