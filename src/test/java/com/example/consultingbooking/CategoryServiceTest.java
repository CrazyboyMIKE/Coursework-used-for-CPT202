package com.example.consultingbooking;

import com.example.consultingbooking.dto.CategoryDtos;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.CategoryService;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CategoryServiceTest {

    private ExpertiseCategoryRepository categoryRepository;
    private AuthService authService;
    private CategoryService categoryService;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        categoryRepository = Mockito.mock(ExpertiseCategoryRepository.class);
        authService = Mockito.mock(AuthService.class);
        categoryService = new CategoryService(categoryRepository, authService);
        admin = new UserAccount();
        admin.setRole(UserRole.ADMIN);
    }

    @Test
    void createCategoryDefaultsActiveToTrue() {
        Mockito.when(categoryRepository.findByNameIgnoreCase("Career Coaching")).thenReturn(Optional.empty());
        Mockito.when(categoryRepository.save(Mockito.any(ExpertiseCategory.class))).thenAnswer(invocation -> {
            ExpertiseCategory category = invocation.getArgument(0);
            category.setId(10L);
            return category;
        });

        CategoryDtos.CategoryResponse response = categoryService.createCategory(
                admin,
                new CategoryDtos.CategoryRequest(" Career Coaching ", "Career support", null)
        );

        ArgumentCaptor<ExpertiseCategory> captor = ArgumentCaptor.forClass(ExpertiseCategory.class);
        Mockito.verify(authService).ensureRole(admin, UserRole.ADMIN);
        Mockito.verify(categoryRepository).save(captor.capture());
        Assertions.assertEquals("Career Coaching", captor.getValue().getName());
        Assertions.assertTrue(captor.getValue().isActive());
        Assertions.assertTrue(response.active());
    }

    @Test
    void createCategoryRejectsDuplicateNameIgnoringCase() {
        ExpertiseCategory existing = new ExpertiseCategory();
        existing.setId(3L);
        existing.setName("Career Coaching");
        Mockito.when(categoryRepository.findByNameIgnoreCase("Career Coaching")).thenReturn(Optional.of(existing));

        Assertions.assertThrows(BusinessException.class, () -> categoryService.createCategory(
                admin,
                new CategoryDtos.CategoryRequest(" Career Coaching ", "Duplicate", true)
        ));

        Mockito.verify(categoryRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void updateCategoryRejectsDuplicateNameFromDifferentCategory() {
        ExpertiseCategory current = new ExpertiseCategory();
        current.setId(5L);
        current.setName("Leadership");
        ExpertiseCategory duplicate = new ExpertiseCategory();
        duplicate.setId(6L);
        duplicate.setName("Strategy");

        Mockito.when(categoryRepository.findById(5L)).thenReturn(Optional.of(current));
        Mockito.when(categoryRepository.findByNameIgnoreCase("Strategy")).thenReturn(Optional.of(duplicate));

        Assertions.assertThrows(BusinessException.class, () -> categoryService.updateCategory(
                admin,
                5L,
                new CategoryDtos.CategoryRequest("Strategy", "Duplicate", true)
        ));

        Mockito.verify(categoryRepository, Mockito.never()).save(Mockito.any());
    }
}
