package com.opportunityboard.interface_http.controller;

import com.opportunityboard.domain.entity.Category;
import com.opportunityboard.infrastructure.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /** Danh sách category (public) — dùng cho form đăng tin. */
    @GetMapping
    public List<Category> list() {
        return categoryRepository.findAll();
    }
}
