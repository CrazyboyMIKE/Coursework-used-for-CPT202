package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.CategoryDtos;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final ExpertiseCategoryRepository expertiseCategoryRepository;
    private final AuthService authService;

    public CategoryService(ExpertiseCategoryRepository expertiseCategoryRepository, AuthService authService) {
        this.expertiseCategoryRepository = expertiseCategoryRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> listCategories() {
        return expertiseCategoryRepository.findAll().stream()
                .map(this::mapCategory)
                .toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse createCategory(UserAccount operator, CategoryDtos.CategoryRequest request) {
        authService.ensureRole(operator, UserRole.ADMIN);

        expertiseCategoryRepository.findByNameIgnoreCase(request.name().trim())
                .ifPresent(category -> {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Category name already exists");
                });

        ExpertiseCategory category = new ExpertiseCategory();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active() == null || request.active());
        return mapCategory(expertiseCategoryRepository.save(category));
    }

    @Transactional
    public CategoryDtos.CategoryResponse updateCategory(
            UserAccount operator,
            Long categoryId,
            CategoryDtos.CategoryRequest request
    ) {
        authService.ensureRole(operator, UserRole.ADMIN);

        ExpertiseCategory category = getEntity(categoryId);
        expertiseCategoryRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Category name already exists");
                });

        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active() == null || request.active());
        return mapCategory(expertiseCategoryRepository.save(category));
    }

    public ExpertiseCategory getEntity(Long categoryId) {
        return expertiseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private CategoryDtos.CategoryResponse mapCategory(ExpertiseCategory category) {
        return new CategoryDtos.CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }
}
