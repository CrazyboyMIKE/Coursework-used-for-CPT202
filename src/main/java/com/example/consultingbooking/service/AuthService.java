package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.AuthDtos;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.PasswordResetToken;
import com.example.consultingbooking.entity.SessionToken;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.PasswordResetTokenRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import com.example.consultingbooking.security.PasswordHasher;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final String AUTH_HEADER = "X-Auth-Token";

    private final UserAccountRepository userAccountRepository;
    private final SessionTokenRepository sessionTokenRepository;
    private final ExpertiseCategoryRepository expertiseCategoryRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthService(
            UserAccountRepository userAccountRepository,
            SessionTokenRepository sessionTokenRepository,
            ExpertiseCategoryRepository expertiseCategoryRepository,
            SpecialistProfileRepository specialistProfileRepository,
            PasswordResetTokenRepository passwordResetTokenRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.sessionTokenRepository = sessionTokenRepository;
        this.expertiseCategoryRepository = expertiseCategoryRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        validateUniqueUser(request.username(), request.email());
        UserAccount saved = userAccountRepository.save(buildUser(
                request.username(),
                request.password(),
                request.fullName(),
                request.email(),
                request.phone(),
                UserRole.CUSTOMER
        ));
        return issueSession(saved);
    }

    @Transactional
    public AuthDtos.AuthResponse registerSpecialist(AuthDtos.SpecialistRegisterRequest request) {
        validateUniqueUser(request.username(), request.email());

        ExpertiseCategory category = resolveOrCreateCategory(request.categoryName());

        UserAccount saved = userAccountRepository.save(buildUser(
                request.username(),
                request.password(),
                request.fullName(),
                request.email(),
                request.phone(),
                UserRole.SPECIALIST
        ));

        SpecialistProfile profile = new SpecialistProfile();
        profile.setUser(saved);
        profile.setCategory(category);
        profile.setLevel(TextNormalizer.cleanRequired(request.level(), "Professional title or certification is required"));
        profile.setBaseFee(request.baseFee());
        profile.setFeeCurrency(BusinessConstants.DEFAULT_CURRENCY);
        profile.setStatus(SpecialistStatus.ACTIVE);
        profile.setBio(TextNormalizer.cleanRequired(request.bio(), "Notes are required"));
        specialistProfileRepository.save(profile);

        return issueSession(saved);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String credential = request.username().trim();
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(credential)
                .or(() -> userAccountRepository.findByEmailIgnoreCase(credential))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password"));

        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        if (!PasswordHasher.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }
        if (PasswordHasher.needsHash(user.getPassword())) {
            user.setPassword(PasswordHasher.hash(request.password()));
            userAccountRepository.save(user);
        }

        return issueSession(user);
    }

    @Transactional
    public void logout(String token) {
        SessionToken session = sessionTokenRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        sessionTokenRepository.delete(session);
    }

    @Transactional
    public void changePassword(String token, AuthDtos.ChangePasswordRequest request) {
        UserAccount user = requireUser(token);

        if (!PasswordHasher.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (PasswordHasher.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }

        user.setPassword(PasswordHasher.hash(request.newPassword()));
        userAccountRepository.save(user);
        invalidateSessions(user.getId());
    }

    @Transactional
    public AuthDtos.PasswordResetRequestResponse requestPasswordReset(AuthDtos.PasswordResetRequest request) {
        String identifier = request.identifier().trim();
        UserAccount user = findUserByIdentifier(identifier)
                .orElse(null);

        if (user == null || !user.isActive()) {
            return new AuthDtos.PasswordResetRequestResponse(
                    "If the account exists, a one-time reset code has been issued.",
                    null,
                    null
            );
        }

        deactivateResetTokens(user.getId());

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(generateResetCode());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        PasswordResetToken saved = passwordResetTokenRepository.save(token);

        return new AuthDtos.PasswordResetRequestResponse(
                "A one-time reset code has been issued. Enter it on the reset page before it expires.",
                saved.getToken(),
                saved.getExpiresAt()
        );
    }

    @Transactional
    public void confirmPasswordReset(AuthDtos.PasswordResetConfirmRequest request) {
        String resetCode = request.resetCode().trim().toUpperCase();
        PasswordResetToken token = passwordResetTokenRepository.findByTokenAndActiveTrue(resetCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "The reset code is invalid or has already been used"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setActive(false);
            passwordResetTokenRepository.save(token);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "The reset code has expired");
        }

        token.setActive(false);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        UserAccount user = token.getUser();
        user.setPassword(PasswordHasher.hash(request.newPassword()));
        userAccountRepository.save(user);
        invalidateSessions(user.getId());
        deactivateResetTokens(user.getId());
    }

    @Transactional
    public UserAccount requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }

        SessionToken session = sessionTokenRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        session.setLastUsedAt(LocalDateTime.now());
        sessionTokenRepository.save(session);

        UserAccount user = userAccountRepository.findById(session.getUser().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "User account not found"));

        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        return user;
    }

    public UserAccount requireRole(String token, UserRole... roles) {
        UserAccount user = requireUser(token);
        ensureRole(user, roles);
        return user;
    }

    public void ensureRole(UserAccount user, UserRole... roles) {
        boolean matched = Arrays.stream(roles).anyMatch(role -> role == user.getRole());
        if (!matched) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Current user does not have the required role");
        }
    }

    private void validateUniqueUser(String username, String email) {
        if (userAccountRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        if (userAccountRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
    }

    private UserAccount buildUser(
            String username,
            String password,
            String fullName,
            String email,
            String phone,
            UserRole role
    ) {
        UserAccount user = new UserAccount();
        user.setUsername(username.trim());
        user.setPassword(PasswordHasher.hash(password));
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(TextNormalizer.cleanOptional(phone));
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private ExpertiseCategory resolveOrCreateCategory(String rawCategoryName) {
        String categoryName = TextNormalizer.cleanRequired(rawCategoryName, "Category is required");
        ExpertiseCategory category = expertiseCategoryRepository.findByNameIgnoreCase(categoryName)
                .orElseGet(ExpertiseCategory::new);
        category.setName(categoryName);
        if (category.getDescription() == null || category.getDescription().isBlank()) {
            category.setDescription("Created during specialist registration.");
        }
        category.setActive(true);
        return expertiseCategoryRepository.save(category);
    }

    private java.util.Optional<UserAccount> findUserByIdentifier(String identifier) {
        return userAccountRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userAccountRepository.findByEmailIgnoreCase(identifier));
    }

    private void invalidateSessions(Long userId) {
        List<SessionToken> sessions = sessionTokenRepository.findByUserIdAndActiveTrue(userId);
        sessions.forEach(session -> session.setActive(false));
        sessionTokenRepository.saveAll(sessions);
    }

    private void deactivateResetTokens(Long userId) {
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserIdAndActiveTrue(userId);
        tokens.forEach(token -> token.setActive(false));
        passwordResetTokenRepository.saveAll(tokens);
    }

    private String generateResetCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private AuthDtos.AuthResponse issueSession(UserAccount user) {
        SessionToken sessionToken = new SessionToken();
        sessionToken.setToken(UUID.randomUUID().toString());
        sessionToken.setUser(user);
        sessionToken.setActive(true);

        SessionToken saved = sessionTokenRepository.save(sessionToken);
        return new AuthDtos.AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                saved.getToken()
        );
    }
}
