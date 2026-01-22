package com.example.book_talker_backend.review.dao;


import com.example.book_talker_backend.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
