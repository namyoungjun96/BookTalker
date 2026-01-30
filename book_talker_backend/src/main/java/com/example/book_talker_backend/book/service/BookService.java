package com.example.book_talker_backend.book.service;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinResponse;
import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.entity.dto.ListRequest;
import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {
    private final RestTemplate restTemplate;
    private final BookRepository bookRepository;

    private final static String ALADIN_BASE_URL = "http://www.aladin.co.kr/ttb/api";

    public AladinResponse list(ListRequest request) {
        String url = UriComponentsBuilder.fromUriString(ALADIN_BASE_URL + "/ItemList.aspx")
                .queryParam("TTBKey", "ttbehdgornltls1927001")
                .queryParam("QueryType", request.queryType().getValue())
                .queryParam("SearchTarget", "Book")
                .queryParam("Start", request.start())
                .queryParam("MaxResults", request.maxResults())
                .queryParam("cover", request.cover())
                .queryParam("Output", "js")
                .queryParam("Version", "20131101")
                .toUriString();

        return restTemplate.getForObject(url, AladinResponse.class);
    }

    public AladinResponse search(SearchRequest request) {
        String url = UriComponentsBuilder
                .fromUriString(ALADIN_BASE_URL + "/ItemSearch.aspx")
                .queryParam("TTBKey", "ttbehdgornltls1927001")
                .queryParam("Query", request.query())
                .queryParam("QueryType", "Title")
                .queryParam("SearchTarget", "Book")
                .queryParam("Start", request.start())
                .queryParam("MaxResults", request.maxResults())
                .queryParam("Cover", request.cover())
                .queryParam("Output", "js")
                .queryParam("InputEncoding", "utf-8")
                .queryParam("Version", "20131101")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, AladinResponse.class);
    }

    public Book getBookByIsbn13WithApi(String isbn13) {
        AladinResponse response = searchByIsbn13(isbn13);

        if(response == null) {
            throw new IllegalArgumentException("[BookTalker] Book with ISBN13 " + isbn13 + " does not exist");
        }

        List<AladinBook> aladinBooks = response.item();

        if (aladinBooks.isEmpty()) {
            throw new IllegalArgumentException("[Aladin] Book with ISBN13 " + isbn13 + " does not exist");
        }

        return aladinBooks.get(0).to();
    }

    public AladinResponse searchByIsbn13(String isbn13) {
        String url = UriComponentsBuilder
                .fromUriString(ALADIN_BASE_URL + "/ItemLookUp.aspx")
                .queryParam("TTBKey", "ttbehdgornltls1927001")
                .queryParam("ItemIdType", "ISBN13")
                .queryParam("ItemId", isbn13)
                .queryParam("Cover", "Small")
                .queryParam("Output", "js")
                .queryParam("InputEncoding", "utf-8")
                .queryParam("Version", "20131101")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, AladinResponse.class);
    }

    public Book getBookByIsbn13(String isbn13) {
        return bookRepository.findById(isbn13).orElse(null);
    }

    public void cacheBook(AladinBook aladinBook) {
        try {
            Book exsitingBook = getBookByIsbn13(aladinBook.isbn13());

            if (exsitingBook == null) {
                Book book = new Book();
                book.setIsbn13(aladinBook.isbn13());
                book.setTitle(aladinBook.title());
                book.setAuthor(aladinBook.author());
                book.setGenre(aladinBook.categoryName());
                book.setPublisher(aladinBook.publisher());
                book.setCover(aladinBook.cover());

                insertBook(book);
            }
        } catch (IllegalArgumentException | OptimisticLockingFailureException e) {
            log.warn("Failed to cache book, but continuing: {}", aladinBook.isbn13(), e);
        }
    }

    public List<Book> insertAllBooks(List<Book> books) {
        return bookRepository.saveAll(books);
    }

    public void insertBook(Book book) {
        bookRepository.save(book);
    }
}
