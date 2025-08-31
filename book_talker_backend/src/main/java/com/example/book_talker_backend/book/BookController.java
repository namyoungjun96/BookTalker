package com.example.book_talker_backend.book;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/book")
public class BookController {
    private final BookRepository bookRepository;

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> bookList = bookRepository.findAll();

        return ResponseEntity.ok(bookList);
    }

    @PostMapping
    public ResponseEntity<Void> crate(Book book) {
        bookRepository.save(book);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Void> update(Book book) {
        bookRepository.save(book);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody Book book) {
        bookRepository.delete(book);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
