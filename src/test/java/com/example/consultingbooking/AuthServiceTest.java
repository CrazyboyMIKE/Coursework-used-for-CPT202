package com.example.consultingbooking;

import com.example.consultingbooking.dto.AuthDtos;
import com.example.consultingbooking.entity.SessionToken;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.PasswordResetTokenRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import com.example.consultingbooking.security.PasswordHasher;
import com.example.consultingbooking.service.AuthService;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthServiceTest {

    private UserAccountRepository userAccountRepository;
    private SessionTokenRepository sessionTokenRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userAccountRepository = Mockito.mock(UserAccountRepository.class);
        sessionTokenRepository = Mockito.mock(SessionTokenRepository.class);
        authService = new AuthService(
                userAccountRepository,
                sessionTokenRepository,
                Mockito.mock(ExpertiseCategoryRepository.class),
                Mockito.mock(SpecialistProfileRepository.class),
                Mockito.mock(PasswordResetTokenRepository.class)
        );
    }

    @Test
    void registerRejectsAnExistingPhoneNumber() {
        Mockito.when(userAccountRepository.existsByPhone("18800001111")).thenReturn(true);

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> authService.register(
                new AuthDtos.RegisterRequest(
                        "customer-one",
                        "password123",
                        "Customer One",
                        "customer-one@example.com",
                        " 18800001111 "
                )
        ));

        Assertions.assertEquals("Phone number already exists", exception.getMessage());
        Mockito.verify(userAccountRepository, Mockito.never()).save(Mockito.any(UserAccount.class));
    }

    @Test
    void loginAcceptsPhoneNumberAsCredential() {
        UserAccount user = new UserAccount();
        user.setId(21L);
        user.setUsername("customer-one");
        user.setPassword(PasswordHasher.hash("password123"));
        user.setFullName("Customer One");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        Mockito.when(userAccountRepository.findByUsernameIgnoreCase("18800001111")).thenReturn(Optional.empty());
        Mockito.when(userAccountRepository.findByEmailIgnoreCase("18800001111")).thenReturn(Optional.empty());
        Mockito.when(userAccountRepository.findByPhone("18800001111")).thenReturn(Optional.of(user));
        Mockito.when(sessionTokenRepository.save(Mockito.any(SessionToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthDtos.AuthResponse response = authService.login(new AuthDtos.LoginRequest("18800001111", "password123"));

        Assertions.assertEquals("customer-one", response.username());
        Assertions.assertFalse(response.token().isBlank());
        Mockito.verify(userAccountRepository).findByPhone("18800001111");
    }
}
