package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.ReportDtos;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.repository.BookingRepository;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {

    private final UserAccountRepository userAccountRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final AuthService authService;

    public ReportingService(
            UserAccountRepository userAccountRepository,
            SpecialistProfileRepository specialistProfileRepository,
            TimeSlotRepository timeSlotRepository,
            BookingRepository bookingRepository,
            AuthService authService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.specialistProfileRepository = specialistProfileRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.bookingRepository = bookingRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public ReportDtos.SummaryResponse summary(UserAccount operator) {
        authService.ensureRole(operator, UserRole.ADMIN);

        Map<String, Long> bookingsByStatus = Arrays.stream(BookingStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        status -> bookingRepository.findAll().stream()
                                .filter(booking -> booking.getStatus() == status)
                                .count()
                ));

        long totalAvailableSlots = timeSlotRepository.findAll().stream()
                .filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE)
                .count();

        BigDecimal revenue = bookingRepository.totalConfirmedRevenue();

        return new ReportDtos.SummaryResponse(
                userAccountRepository.count(),
                specialistProfileRepository.count(),
                totalAvailableSlots,
                bookingRepository.count(),
                revenue,
                bookingsByStatus
        );
    }
}
