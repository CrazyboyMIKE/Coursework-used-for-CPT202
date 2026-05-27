package com.example.consultingbooking;

import com.example.consultingbooking.dto.UserDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserServiceTest {

    private UserAccountRepository userAccountRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userAccountRepository = Mockito.mock(UserAccountRepository.class);
        userService = new UserService(
                userAccountRepository,
                Mockito.mock(SpecialistProfileRepository.class),
                Mockito.mock(AuthService.class)
        );
    }

    @Test
    void profileUpdateRejectsPhoneAlreadyOwnedByAnotherUser() {
        UserAccount current = new UserAccount();
        current.setId(5L);
        current.setEmail("current@example.com");

        UserAccount existing = new UserAccount();
        existing.setId(6L);
        Mockito.when(userAccountRepository.findByPhone("18800001111")).thenReturn(Optional.of(existing));

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> userService.updateCurrentUser(
                current,
                new UserDtos.UpdateProfileRequest("Current User", "current@example.com", "18800001111")
        ));

        Assertions.assertEquals("Phone number already exists", exception.getMessage());
        Mockito.verify(userAccountRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void profileUpdateAllowsKeepingOwnPhoneNumber() {
        UserAccount current = new UserAccount();
        current.setId(5L);
        current.setEmail("current@example.com");
        current.setPhone("18800001111");
        current.setRole(UserRole.CUSTOMER);
        Mockito.when(userAccountRepository.findByPhone("18800001111")).thenReturn(Optional.of(current));
        Mockito.when(userAccountRepository.save(current)).thenReturn(current);

        UserDtos.UserResponse response = userService.updateCurrentUser(
                current,
                new UserDtos.UpdateProfileRequest("Current User", "current@example.com", " 18800001111 ")
        );

        Assertions.assertEquals("18800001111", response.phone());
        Mockito.verify(userAccountRepository).save(current);
    }
}
