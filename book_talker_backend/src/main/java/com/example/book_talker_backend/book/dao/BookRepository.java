package com.example.book_talker_backend.book.dao;

import com.example.book_talker_backend.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
