package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.PageDtos;
import com.example.consultingbooking.dto.UserDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import com.example.consultingbooking.security.PasswordHasher;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final AuthService authService;

    public UserService(
            UserAccountRepository userAccountRepository,
            SpecialistProfileRepository specialistProfileRepository,
            AuthService authService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse currentUser(UserAccount currentUser) {
        return mapUser(currentUser);
    }

    @Transactional
    public UserDtos.UserResponse createUser(UserAccount operator, UserDtos.CreateUserRequest request) {
        return mapUser(createUserEntity(operator, request));
    }

    @Transactional
    public UserAccount createUserEntity(UserAccount operator, UserDtos.CreateUserRequest request) {
        authService.ensureRole(operator, UserRole.ADMIN);

        if (userAccountRepository.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userAccountRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        validateAvailablePhone(request.phone(), null);

        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setPassword(PasswordHasher.hash(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(TextNormalizer.cleanOptional(request.phone()));
        user.setRole(request.role());
        user.setActive(true);
        return userAccountRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> listUsers(UserAccount operator) {
        authService.ensureRole(operator, UserRole.ADMIN);
        return userAccountRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::mapUser)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageDtos.PageResponse<UserDtos.UserResponse> listUsers(
            UserAccount operator,
            String keyword,
            Pageable pageable
    ) {
        authService.ensureRole(operator, UserRole.ADMIN);
        return PageDtos.PageResponse.from(userAccountRepository.searchForAdmin(TextNormalizer.keyword(keyword), pageable)
                .map(this::mapUser));
    }

    @Transactional
    public UserDtos.UserResponse updateUser(UserAccount operator, Long userId, UserDtos.AdminUpdateUserRequest request) {
        authService.ensureRole(operator, UserRole.ADMIN);

        UserAccount user = getEntity(userId);
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (!user.getUsername().equalsIgnoreCase(username) && userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (!user.getEmail().equalsIgnoreCase(email) && userAccountRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        validateAvailablePhone(request.phone(), userId);
        if (specialistProfileRepository.findByUserId(userId).isPresent() && request.role() != UserRole.SPECIALIST) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Users with specialist profiles must keep the SPECIALIST role");
        }

        user.setUsername(username);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(TextNormalizer.cleanOptional(request.phone()));
        user.setRole(request.role());
        user.setActive(request.active());
        return mapUser(userAccountRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse updateCurrentUser(UserAccount currentUser, UserDtos.UpdateProfileRequest request) {
        String email = request.email().trim().toLowerCase();
        if (!currentUser.getEmail().equalsIgnoreCase(email) && userAccountRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        validateAvailablePhone(request.phone(), currentUser.getId());

        currentUser.setFullName(request.fullName().trim());
        currentUser.setEmail(email);
        currentUser.setPhone(TextNormalizer.cleanOptional(request.phone()));
        return mapUser(userAccountRepository.save(currentUser));
    }

    @Transactional
    public void resetPassword(UserAccount operator, Long userId, UserDtos.AdminResetPasswordRequest request) {
        authService.ensureRole(operator, UserRole.ADMIN);

        UserAccount user = getEntity(userId);
        user.setPassword(PasswordHasher.hash(request.newPassword()));
        userAccountRepository.save(user);
        authService.invalidateSessions(userId);
    }

    public UserAccount getEntity(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateAvailablePhone(String phone, Long currentUserId) {
        String normalizedPhone = TextNormalizer.cleanOptional(phone);
        if (normalizedPhone == null) {
            return;
        }

        userAccountRepository.findByPhone(normalizedPhone)
                .filter(user -> !Objects.equals(user.getId(), currentUserId))
                .ifPresent(user -> {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Phone number already exists");
                });
    }

    private UserDtos.UserResponse mapUser(UserAccount user) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

}
