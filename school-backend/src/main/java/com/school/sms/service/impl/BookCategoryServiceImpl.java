package com.school.sms.service.impl;

import com.school.sms.dto.request.BookCategoryRequest;
import com.school.sms.dto.response.BookCategoryDto;
import com.school.sms.entity.BookCategory;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.BookCategoryMapper;
import com.school.sms.repository.BookCategoryRepository;
import com.school.sms.service.BookCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookCategoryServiceImpl implements BookCategoryService {

    private final BookCategoryRepository bookCategoryRepository;
    private final BookCategoryMapper bookCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BookCategoryDto> getAll() {
        return bookCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(bookCategoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BookCategoryDto create(BookCategoryRequest request) {
        if (bookCategoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("BookCategory", "name", request.getName());
        }
        BookCategory entity = bookCategoryMapper.toEntity(request);
        return bookCategoryMapper.toDto(bookCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public BookCategoryDto update(Long id, BookCategoryRequest request) {
        BookCategory entity = findEntity(id);
        if (!entity.getName().equalsIgnoreCase(request.getName())
                && bookCategoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("BookCategory", "name", request.getName());
        }
        bookCategoryMapper.updateEntityFromRequest(request, entity);
        return bookCategoryMapper.toDto(bookCategoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        bookCategoryRepository.delete(findEntity(id));
    }

    private BookCategory findEntity(Long id) {
        return bookCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCategory", "id", id));
    }
}
