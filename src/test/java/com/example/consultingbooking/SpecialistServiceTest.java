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
    private SpecialistService specialistService;
    private UserAccount specialistUser;
    private SpecialistProfile profile;

    @BeforeEach
    void setUp() {
        specialistProfileRepository = Mockito.mock(SpecialistProfileRepository.class);
        categoryService = Mockito.mock(CategoryService.class);
        authService = Mockito.mock(AuthService.class);
        specialistService = new SpecialistService(
                specialistProfileRepository,
                Mockito.mock(TimeSlotRepository.class),
                Mockito.mock(UserService.class),
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
}
