package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.CategoryDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    public List<CategoryDtos.CategoryResponse> list() {
        return categoryService.listCategories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtos.CategoryResponse create(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody CategoryDtos.CategoryRequest request
    ) {
        UserAccount operator = authService.requireUser(token);
        return categoryService.createCategory(operator, request);
    }

    @PutMapping("/{id}")
    public CategoryDtos.CategoryResponse update(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @Valid @RequestBody CategoryDtos.CategoryRequest request
    ) {
        UserAccount operator = authService.requireUser(token);
        return categoryService.updateCategory(operator, id, request);
    }
}
