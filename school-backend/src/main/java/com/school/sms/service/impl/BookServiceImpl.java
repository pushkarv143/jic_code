package com.school.sms.service.impl;

import com.school.sms.dto.request.BookRequest;
import com.school.sms.dto.response.BookDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.Book;
import com.school.sms.entity.BookCategory;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.BookMapper;
import com.school.sms.repository.BookCategoryRepository;
import com.school.sms.repository.BookRepository;
import com.school.sms.service.BookService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookMapper bookMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookDto> getAll(String search, Long categoryId, Pageable pageable) {
        Specification<Book> spec = new SpecificationBuilder<Book>()
                .with("deleted", SearchOperation.EQUALS, false)
                .with(categoryId != null, "category.id", SearchOperation.EQUALS, categoryId)
                .build();

        if (StringUtils.hasText(search)) {
            String term = search.trim().toLowerCase();
            Specification<Book> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("author")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("isbn")), "%" + term + "%")
            );
            spec = spec == null ? searchSpec : spec.and(searchSpec);
        }

        Page<Book> page = bookRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(bookMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public BookDto getById(Long id) {
        return bookMapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public BookDto create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException("Book", "isbn", request.getIsbn());
        }
        BookCategory category = findCategory(request.getCategoryId());

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .category(category)
                .publisher(request.getPublisher())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
                .rackNumber(request.getRackNumber())
                .price(request.getPrice())
                .deleted(false)
                .build();

        return bookMapper.toDto(bookRepository.save(book));
    }

    @Override
    @Transactional
    public BookDto update(Long id, BookRequest request) {
        Book book = findEntity(id);

        if (!book.getIsbn().equalsIgnoreCase(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException("Book", "isbn", request.getIsbn());
        }
        BookCategory category = findCategory(request.getCategoryId());

        // available_copies is otherwise trigger-maintained (issue/return), so a
        // manual total_copies change here only nudges it by the same delta,
        // never overwrites it outright, and never lets it go negative.
        int delta = request.getTotalCopies() - book.getTotalCopies();
        int newAvailable = Math.max(0, book.getAvailableCopies() + delta);

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setCategory(category);
        book.setPublisher(request.getPublisher());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(newAvailable);
        book.setRackNumber(request.getRackNumber());
        book.setPrice(request.getPrice());

        return bookMapper.toDto(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Book book = findEntity(id);
        book.setDeleted(true);
        bookRepository.save(book);
    }

    private Book findEntity(Long id) {
        return bookRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    private BookCategory findCategory(Long id) {
        return bookCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCategory", "id", id));
    }
}
