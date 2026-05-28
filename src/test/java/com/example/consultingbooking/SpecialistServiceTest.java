package com.example.consultingbooking;

import com.example.consultingbooking.dto.SpecialistDtos;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.CategoryService;
import com.example.consultingbooking.service.SpecialistService;
import com.example.consultingbooking.service.UserService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SpecialistServiceTest {

    private SpecialistProfileRepository specialistProfileRepository;
    private CategoryService categoryService;
    private AuthService authService;
    private UserService userService;
    private SpecialistService specialistService;
    private UserAccount specialistUser;
    private SpecialistProfile profile;

    @BeforeEach
    void setUp() {
        specialistProfileRepository = Mockito.mock(SpecialistProfileRepository.class);
        categoryService = Mockito.mock(CategoryService.class);
        authService = Mockito.mock(AuthService.class);
        userService = Mockito.mock(UserService.class);
        specialistService = new SpecialistService(
                specialistProfileRepository,
                Mockito.mock(TimeSlotRepository.class),
                userService,
                categoryService,
                authService
        );

        specialistUser = new UserAccount();
        specialistUser.setId(20L);
        specialistUser.setFullName("Specialist One");
        specialistUser.setRole(UserRole.SPECIALIST);

        ExpertiseCategory category = new ExpertiseCategory();
        category.setId(1L);
        category.setName("Finance");

        profile = new SpecialistProfile();
        profile.setId(4L);
        profile.setUser(specialistUser);
        profile.setCategory(category);
        profile.setLevel("Advisor");
        profile.setBaseFee(new BigDecimal("100.00"));
        profile.setStatus(SpecialistStatus.INACTIVE);
    }

    @Test
    void specialistCanUpdateOwnProfessionalDataWithoutChangingAdminStatus() {
        ExpertiseCategory updatedCategory = new ExpertiseCategory();
        updatedCategory.setId(2L);
        updatedCategory.setName("Legal");
        Mockito.when(specialistProfileRepository.findByUserId(20L)).thenReturn(Optional.of(profile));
        Mockito.when(categoryService.getEntity(2L)).thenReturn(updatedCategory);
        Mockito.when(specialistProfileRepository.save(profile)).thenReturn(profile);

        SpecialistDtos.SpecialistResponse response = specialistService.updateCurrentSpecialist(
                specialistUser,
                new SpecialistDtos.SpecialistSelfUpdateRequest(
                        2L,
                        "Licensed Advisor",
                        new BigDecimal("155.00"),
                        "USD",
                        "Updated notes"
                )
        );

        Mockito.verify(authService).ensureRole(specialistUser, UserRole.SPECIALIST);
        Assertions.assertEquals("Legal", response.categoryName());
        Assertions.assertEquals("Licensed Advisor", response.level());
        Assertions.assertEquals(new BigDecimal("155.00"), response.baseFee());
        Assertions.assertEquals(SpecialistStatus.INACTIVE, response.status());
    }

    @Test
    void adminCanCreateSpecialistAccountAndProfileInOneCall() {
        UserAccount admin = new UserAccount();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        UserAccount createdUser = new UserAccount();
        createdUser.setId(30L);
        createdUser.setFullName("Created Specialist");
        createdUser.setRole(UserRole.SPECIALIST);

        ExpertiseCategory category = new ExpertiseCategory();
        category.setId(2L);
        category.setName("Legal");

        Mockito.when(userService.createUserEntity(Mockito.eq(admin), Mockito.any()))
                .thenReturn(createdUser);
        Mockito.when(categoryService.getEntity(2L)).thenReturn(category);
        Mockito.when(specialistProfileRepository.save(Mockito.any(SpecialistProfile.class)))
                .thenAnswer(invocation -> {
                    SpecialistProfile saved = invocation.getArgument(0);
                    saved.setId(9L);
                    return saved;
                });

        SpecialistDtos.SpecialistResponse response = specialistService.createSpecialistAccount(
                admin,
                new SpecialistDtos.SpecialistAccountRequest(
                        "new-specialist",
                        "Password123",
                        "Created Specialist",
                        "created@example.com",
                        "18800009999",
                        2L,
                        "Trial Consultant",
                        new BigDecimal("210.00"),
                        "USD",
                        SpecialistStatus.ACTIVE,
                        "Created in one admin action"
                )
        );

        Mockito.verify(authService).ensureRole(admin, UserRole.ADMIN);
        Mockito.verify(userService).createUserEntity(Mockito.eq(admin), Mockito.argThat(request ->
                request.role() == UserRole.SPECIALIST
                        && request.username().equals("new-specialist")
                        && request.email().equals("created@example.com")
        ));
        Assertions.assertEquals(9L, response.id());
        Assertions.assertEquals(30L, response.userId());
        Assertions.assertEquals("Legal", response.categoryName());
        Assertions.assertEquals(SpecialistStatus.ACTIVE, response.status());
    }
}
