package com.example.book_talker_backend.book.service;

import com.example.book_talker_backend.book.cache.SearchCacheKey;
import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.entity.dto.AladinResponse;
import com.example.book_talker_backend.book.entity.dto.BookSearchResponse;
import com.example.book_talker_backend.book.entity.dto.CoverSizeEnum;
import com.example.book_talker_backend.book.entity.dto.ListRequest;
import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import com.example.book_talker_backend.book.exception.AladinApiException;
import com.example.book_talker_backend.book.infrastructure.AladinBookMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {
    private final RestTemplate restTemplate;
    private final Cache<SearchCacheKey, AladinResponse> searchCache;
    private final BookRepository bookRepository;
    private static final long PAGE_SIZE = 10;
    private static final String BOOK_COVER_SIZE = CoverSizeEnum.SMALL.getValue();

    @Value("${aladin.api.key}")
    private String aladinApiKey;

    private final static String ALADIN_BASE_URL = "https://www.aladin.co.kr/ttb/api";

    public AladinResponse list(ListRequest request) {
        String urlTemplate = ALADIN_BASE_URL + "/ItemList.aspx?TTBKey={ttbKey}&QueryType={queryType}&SearchTarget=Book"
                + "&Start={start}&MaxResults={maxResults}&Cover={cover}&Output=js&InputEncoding=utf-8&Version=20131101";

        Map<String, Object> params = Map.of(
                "ttbKey", aladinApiKey,
                "queryType", request.queryType().getValue(),
                "start", request.start(),
                "maxResults", PAGE_SIZE,
                "cover", BOOK_COVER_SIZE
        );

        return restTemplate.getForObject(urlTemplate, AladinResponse.class, params);
    }

    public BookSearchResponse search(SearchRequest request) {
        SearchCacheKey queryKey = new SearchCacheKey(request.query(), request.start());
        AladinResponse response = searchCache.get(queryKey, key -> fetchFromAladin(request));

        return new BookSearchResponse(
                response.item(),
                response.totalResults(),
                response.startIndex(),
                (long) request.start() * PAGE_SIZE < response.totalResults()
        );
    }

    private AladinResponse fetchFromAladin(SearchRequest request) {
        String urlTemplate = ALADIN_BASE_URL + "/ItemSearch.aspx?TTBKey={ttbKey}&Query={query}&QueryType=Title&SearchTarget=Book"
                + "&Start={start}&MaxResults={maxResults}&Cover={cover}&Output=js&InputEncoding=utf-8&Version=20131101";
        Map<String, Object> params = Map.of(
                "ttbKey", aladinApiKey,
                "query", request.query(),
                "start", request.start(),
                "maxResults", PAGE_SIZE,
                "cover", BOOK_COVER_SIZE
        );

        AladinResponse response = restTemplate.getForObject(urlTemplate, AladinResponse.class, params);

        if (response == null || response.item() == null)
            throw new AladinApiException("알라딘 검색 응답 오류 (query=" + request.query() + ", start=" + request.start() + ")");

        return response;
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

        return AladinBookMapper.toDomain(aladinBooks.get(0));
    }

    public AladinResponse searchByIsbn13(String isbn13) {
        String urlTemplate = ALADIN_BASE_URL + "/ItemLookUp.aspx?TTBKey={ttbKey}&ItemId={isbn13}&ItemIdType=ISBN13" +
                "&Cover=Small&Output=js&InputEncoding=utf-8&Version=20131101";
        Map<String, Object> params = Map.of(
                "ttbKey", aladinApiKey,
                "isbn13", isbn13
        );

        return restTemplate.getForObject(urlTemplate, AladinResponse.class, params);
    }

    public Book getBookByIsbn13(String isbn13) {
        return bookRepository.findById(isbn13).orElse(null);
    }

    public void cacheBook(AladinBook aladinBook) {
        try {
            Book exsitingBook = getBookByIsbn13(aladinBook.isbn13());

            if (exsitingBook == null) {
                insertBook(AladinBookMapper.toDomain(aladinBook));
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
