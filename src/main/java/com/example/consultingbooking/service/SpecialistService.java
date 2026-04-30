package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.SpecialistDtos;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistLevel;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialistService {

    private final SpecialistProfileRepository specialistProfileRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final AuthService authService;

    public SpecialistService(
            SpecialistProfileRepository specialistProfileRepository,
            TimeSlotRepository timeSlotRepository,
            UserService userService,
            CategoryService categoryService,
            AuthService authService
    ) {
        this.specialistProfileRepository = specialistProfileRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userService = userService;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @Transactional
    public SpecialistDtos.SpecialistResponse createSpecialist(
            UserAccount operator,
            SpecialistDtos.SpecialistRequest request
    ) {
        authService.ensureRole(operator, UserRole.ADMIN);

        UserAccount specialistUser = userService.getEntity(request.userId());
        if (specialistUser.getRole() != UserRole.SPECIALIST) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "User must have SPECIALIST role");
        }

        specialistProfileRepository.findByUserId(specialistUser.getId())
                .ifPresent(profile -> {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Specialist profile already exists");
                });

        SpecialistProfile profile = new SpecialistProfile();
        profile.setUser(specialistUser);
        profile.setCategory(categoryService.getEntity(request.categoryId()));
        profile.setLevel(request.level());
        profile.setBaseFee(request.baseFee());
        profile.setStatus(request.status());
        profile.setBio(request.bio());
        return mapSpecialist(specialistProfileRepository.save(profile));
    }

    @Transactional
    public SpecialistDtos.SpecialistResponse updateSpecialist(
            UserAccount operator,
            Long specialistId,
            SpecialistDtos.SpecialistUpdateRequest request
    ) {
        authService.ensureRole(operator, UserRole.ADMIN);

        SpecialistProfile profile = getEntity(specialistId);
        profile.setCategory(categoryService.getEntity(request.categoryId()));
        profile.setLevel(request.level());
        profile.setBaseFee(request.baseFee());
        profile.setStatus(request.status());
        profile.setBio(request.bio());
        return mapSpecialist(specialistProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<SpecialistDtos.SpecialistResponse> searchSpecialists(
            Long categoryId,
            SpecialistLevel level,
            String keyword,
            BigDecimal minFee,
            BigDecimal maxFee,
            LocalDateTime availableAt
    ) {
        if (minFee != null && maxFee != null && maxFee.compareTo(minFee) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Maximum fee must be greater than or equal to minimum fee");
        }

        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase();
        return specialistProfileRepository.findAll().stream()
                .filter(profile -> profile.getStatus() == SpecialistStatus.ACTIVE)
                .filter(profile -> categoryId == null || profile.getCategory().getId().equals(categoryId))
                .filter(profile -> level == null || profile.getLevel() == level)
                .filter(profile -> normalizedKeyword == null || normalizedKeyword.isEmpty() || matchesKeyword(profile, normalizedKeyword))
                .filter(profile -> minFee == null || profile.getBaseFee().compareTo(minFee) >= 0)
                .filter(profile -> maxFee == null || profile.getBaseFee().compareTo(maxFee) <= 0)
                .filter(profile -> availableAt == null || hasAvailableSlot(profile, availableAt))
                .map(this::mapSpecialist)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialistDtos.SpecialistResponse> listSpecialistsForManagement(UserAccount operator) {
        authService.ensureRole(operator, UserRole.ADMIN);
        return specialistProfileRepository.findAll().stream()
                .map(this::mapSpecialist)
                .toList();
    }

    public SpecialistProfile getEntity(Long specialistId) {
        return specialistProfileRepository.findById(specialistId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Specialist not found"));
    }

    @Transactional(readOnly = true)
    public SpecialistDtos.SpecialistResponse specialistDetail(Long specialistId) {
        return mapSpecialist(getEntity(specialistId));
    }

    @Transactional(readOnly = true)
    public SpecialistDtos.SpecialistResponse currentSpecialist(UserAccount currentUser) {
        authService.ensureRole(currentUser, UserRole.SPECIALIST);
        SpecialistProfile profile = specialistProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Specialist profile not found"));
        return mapSpecialist(profile);
    }

    public void ensureOwnerOrAdmin(UserAccount actor, SpecialistProfile specialist) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }

        if (actor.getRole() == UserRole.SPECIALIST && actor.getId().equals(specialist.getUser().getId())) {
            return;
        }

        throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot manage this specialist");
    }

    private boolean hasAvailableSlot(SpecialistProfile profile, LocalDateTime availableAt) {
        List<TimeSlot> slots = timeSlotRepository.findBySpecialistIdOrderByStartTimeAsc(profile.getId());
        return slots.stream().anyMatch(slot ->
                slot.getStatus() == SlotStatus.AVAILABLE
                        && !availableAt.isBefore(slot.getStartTime())
                        && availableAt.isBefore(slot.getEndTime())
        );
    }

    private boolean matchesKeyword(SpecialistProfile profile, String keyword) {
        return profile.getUser().getFullName().toLowerCase().contains(keyword)
                || profile.getCategory().getName().toLowerCase().contains(keyword)
                || (profile.getBio() != null && profile.getBio().toLowerCase().contains(keyword));
    }

    private SpecialistDtos.SpecialistResponse mapSpecialist(SpecialistProfile profile) {
        return new SpecialistDtos.SpecialistResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getCategory().getId(),
                profile.getUser().getFullName(),
                profile.getCategory().getName(),
                profile.getLevel(),
                profile.getBaseFee(),
                profile.getStatus(),
                profile.getBio()
        );
    }
}
