package com.example.book_talker_backend.review;

import com.example.book_talker_backend.review.entity.Rank;
import com.example.book_talker_backend.review.entity.dto.PublicReviewResponse;
import com.example.book_talker_backend.review.service.RankService;
import com.example.book_talker_backend.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rank")
public class RankController {
    private final RankService rankService;
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<Rank>> getRank(@RequestParam(required = false) String genre) {
        return new ResponseEntity<>(rankService.getRank(genre), HttpStatus.OK);
    }

    @GetMapping("/genres")
    public ResponseEntity<Map<String, List<Rank>>> getRankByGenre() {
        return new ResponseEntity<>(rankService.getRankByGenre(), HttpStatus.OK);
    }

    @GetMapping("/reviews")
    public ResponseEntity<Page<PublicReviewResponse>> getPublicReviews(
            @RequestParam String isbn13,
            @RequestParam(defaultValue = "0") int page) {
        return new ResponseEntity<>(reviewService.getPublicReviews(isbn13, page), HttpStatus.OK);
    }
}
