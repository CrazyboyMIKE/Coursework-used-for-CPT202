package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.BookingDtos;
import com.example.consultingbooking.dto.PageDtos;
import com.example.consultingbooking.dto.RefundDtos;
import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.BookingRepository;
import com.example.consultingbooking.repository.EvaluationRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Set<BookingStatus> ACTIVE_SLOT_STATUSES = EnumSet.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.COMPLETED
    );

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EvaluationRepository evaluationRepository;
    private final SpecialistService specialistService;
    private final PricingService pricingService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final RefundService refundService;
    private final FeeBreakdownPdfService feeBreakdownPdfService;

    public BookingService(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            EvaluationRepository evaluationRepository,
            SpecialistService specialistService,
            PricingService pricingService,
            AuthService authService,
            NotificationService notificationService,
            RefundService refundService,
            FeeBreakdownPdfService feeBreakdownPdfService
    ) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.evaluationRepository = evaluationRepository;
        this.specialistService = specialistService;
        this.pricingService = pricingService;
        this.authService = authService;
        this.notificationService = notificationService;
        this.refundService = refundService;
        this.feeBreakdownPdfService = feeBreakdownPdfService;
    }

    @Transactional
    public BookingDtos.BookingResponse createBooking(UserAccount customer, BookingDtos.CreateBookingRequest request) {
        authService.ensureRole(customer, UserRole.CUSTOMER);

        SpecialistProfile specialist = specialistService.getEntity(request.specialistId());
        TimeSlot slot = getLockedSlot(request.slotId());
        ensureBookableSpecialist(specialist);
        ensureSlotMatchesSpecialist(slot, specialist);
        ensureSlotAvailable(slot);
        PricingService.PriceCalculation calculation = pricingService.calculateBreakdown(specialist.getBaseFee(), slot);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSpecialist(specialist);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTopic(request.topic().trim());
        booking.setNotes(request.notes());
        booking.setPrice(calculation.totalPrice());
        booking.setUnitPrice(specialist.getBaseFee());
        booking.setPricingMultiplier(calculation.effectiveMultiplier());
        booking.setLastActionReason("Booking created");

        slot.setStatus(SlotStatus.RESERVED);
        timeSlotRepository.save(slot);
        return mapBooking(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingDtos.FeeQuoteResponse quoteBooking(UserAccount customer, BookingDtos.QuoteRequest request) {
        authService.ensureRole(customer, UserRole.CUSTOMER);

        SpecialistProfile specialist = specialistService.getEntity(request.specialistId());
        TimeSlot slot = timeSlotRepository.findById(request.slotId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Time slot not found"));
        ensureBookableSpecialist(specialist);
        ensureSlotMatchesSpecialist(slot, specialist);
        ensureSlotAvailable(slot);

        PricingService.PriceCalculation calculation = pricingService.calculateBreakdown(specialist.getBaseFee(), slot);
        return new BookingDtos.FeeQuoteResponse(
                specialist.getId(),
                specialist.getUser().getFullName(),
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                calculation.durationMinutes(),
                calculation.unitPrice(),
                calculation.effectiveMultiplier(),
                calculation.totalPrice(),
                BusinessConstants.DEFAULT_CURRENCY,
                mapFeeComponents(calculation)
        );
    }

    @Transactional
    public BookingDtos.BookingResponse confirmBooking(UserAccount actor, Long bookingId) {
        Booking booking = getEntity(bookingId);
        ensureManager(actor, booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setLastActionReason("Booking confirmed");
        Booking saved = bookingRepository.save(booking);
        notificationService.notifyConfirmed(saved);
        return mapBooking(saved);
    }

    @Transactional
    public BookingDtos.BookingResponse rejectBooking(UserAccount actor, Long bookingId, String reason) {
        Booking booking = getEntity(bookingId);
        ensureManager(actor, booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only pending bookings can be rejected");
        }

        if (reason == null || reason.trim().length() < 5) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "A rejection reason of at least 5 characters is required");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setLastActionReason(reason.trim());
        releaseSlot(booking.getSlot());
        Booking saved = bookingRepository.save(booking);
        notificationService.notifyRejected(saved);
        return mapBooking(saved);
    }

    @Transactional
    public BookingDtos.BookingResponse cancelBooking(UserAccount actor, Long bookingId, String reason) {
        Booking booking = getEntity(bookingId);
        if (actor.getRole() == UserRole.CUSTOMER) {
            ensureCustomerOwnsBooking(actor, booking);
        } else {
            ensureManager(actor, booking);
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be modified");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Booking is already closed");
        }
        if (actor.getRole() == UserRole.CUSTOMER && !booking.getSlot().getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Customers can only cancel future bookings");
        }
        if (actor.getRole() == UserRole.CUSTOMER
                && booking.getStatus() == BookingStatus.CONFIRMED
                && !booking.getSlot().getStartTime().isAfter(LocalDateTime.now().plusHours(24))) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Confirmed bookings can only be cancelled more than 24 hours before the consultation starts"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setLastActionReason(reason == null || reason.isBlank() ? "Booking cancelled" : reason.trim());
        booking.setCancelledAt(LocalDateTime.now());
        releaseSlot(booking.getSlot());
        Booking saved = bookingRepository.save(booking);
        refundService.synchroniseCancellation(saved);
        notificationService.notifyCancelled(saved, actor);
        return mapBooking(saved);
    }

    @Transactional
    public BookingDtos.BookingResponse rescheduleBooking(
            UserAccount customer,
            Long bookingId,
            BookingDtos.RescheduleRequest request
    ) {
        authService.ensureRole(customer, UserRole.CUSTOMER);

        Booking booking = getEntity(bookingId);
        ensureCustomerOwnsBooking(customer, booking);

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be modified");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Closed bookings cannot be rescheduled");
        }
        if (!booking.getSlot().getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only future bookings can be rescheduled");
        }

        TimeSlot newSlot = getLockedSlot(request.newSlotId());
        if (newSlot.getId().equals(booking.getSlot().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "New slot must be different from current slot");
        }
        ensureBookableSpecialist(newSlot.getSpecialist());
        ensureSlotAvailable(newSlot);
        PricingService.PriceCalculation calculation = pricingService.calculateBreakdown(
                newSlot.getSpecialist().getBaseFee(),
                newSlot
        );

        releaseSlot(booking.getSlot());
        newSlot.setStatus(SlotStatus.RESERVED);
        timeSlotRepository.save(newSlot);

        booking.setSpecialist(newSlot.getSpecialist());
        booking.setSlot(newSlot);
        booking.setPrice(calculation.totalPrice());
        booking.setUnitPrice(newSlot.getSpecialist().getBaseFee());
        booking.setPricingMultiplier(calculation.effectiveMultiplier());
        booking.setStatus(BookingStatus.PENDING);
        booking.setLastActionReason("Booking rescheduled and requires reconfirmation");
        Booking saved = bookingRepository.save(booking);
        notificationService.notifyRescheduled(saved);
        return mapBooking(saved);
    }

    @Transactional
    public BookingDtos.BookingResponse completeBooking(UserAccount actor, Long bookingId) {
        Booking booking = getEntity(bookingId);
        ensureManager(actor, booking);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only confirmed bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setLastActionReason("Booking completed");
        booking.getSlot().setStatus(SlotStatus.CLOSED);
        timeSlotRepository.save(booking.getSlot());
        return mapBooking(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingDtos.BookingResponse> customerBookings(UserAccount customer, BookingStatus status) {
        authService.ensureRole(customer, UserRole.CUSTOMER);
        List<Booking> bookings = status == null
                ? bookingRepository.findByCustomerIdOrderBySlotStartTimeDesc(customer.getId())
                : bookingRepository.findByCustomerIdAndStatusOrderBySlotStartTimeDesc(customer.getId(), status);
        return bookings.stream()
                .map(this::mapBooking)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDtos.BookingResponse> specialistSchedule(UserAccount specialistUser, BookingStatus status) {
        return specialistSchedule(specialistUser, status, null, null);
    }

    @Transactional(readOnly = true)
    public List<BookingDtos.BookingResponse> specialistSchedule(
            UserAccount specialistUser,
            BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        authService.ensureRole(specialistUser, UserRole.SPECIALIST);
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        LocalDateTime rangeStart = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime rangeEnd = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        List<Booking> bookings = bookingRepository.findSpecialistSchedule(
                specialistUser.getId(),
                status,
                rangeStart,
                rangeEnd
        );
        return bookings.stream()
                .map(this::mapBooking)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDtos.BookingResponse> adminBookings(UserAccount admin, BookingStatus status) {
        authService.ensureRole(admin, UserRole.ADMIN);
        List<Booking> bookings = status == null
                ? bookingRepository.findAllByOrderBySlotStartTimeDesc()
                : bookingRepository.findByStatusOrderBySlotStartTimeDesc(status);
        return bookings.stream()
                .map(this::mapBooking)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageDtos.PageResponse<BookingDtos.BookingResponse> adminBookings(
            UserAccount admin,
            BookingStatus status,
            String keyword,
            Pageable pageable
    ) {
        authService.ensureRole(admin, UserRole.ADMIN);
        return PageDtos.PageResponse.from(bookingRepository.searchForAdmin(status, TextNormalizer.keyword(keyword), pageable)
                .map(this::mapBooking));
    }

    @Transactional(readOnly = true)
    public BookingDtos.FeeBreakdownResponse feeBreakdown(UserAccount actor, Long bookingId) {
        Booking booking = getEntity(bookingId);
        ensureCanViewBooking(actor, booking);
        return mapFeeBreakdown(booking);
    }

    @Transactional(readOnly = true)
    public BookingDtos.BookingResponse bookingDetails(UserAccount actor, Long bookingId) {
        Booking booking = getEntity(bookingId);
        ensureCanViewBooking(actor, booking);
        return mapBooking(booking);
    }

    @Transactional(readOnly = true)
    public byte[] feeBreakdownPdf(UserAccount actor, Long bookingId) {
        return feeBreakdownPdfService.create(feeBreakdown(actor, bookingId));
    }

    @Transactional(readOnly = true)
    public RefundDtos.RefundResponse refundDetails(UserAccount actor, Long bookingId) {
        Booking booking = getEntity(bookingId);
        ensureCanViewBooking(actor, booking);
        return refundService.findForBooking(bookingId);
    }

    public Booking getEntity(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void ensureBookableSpecialist(SpecialistProfile specialist) {
        if (specialist.getStatus() == SpecialistStatus.INACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Specialist is inactive");
        }
    }

    private void ensureSlotMatchesSpecialist(TimeSlot slot, SpecialistProfile specialist) {
        if (!slot.getSpecialist().getId().equals(specialist.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selected slot does not belong to the specialist");
        }
    }

    private void ensureSlotAvailable(TimeSlot slot) {
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new BusinessException(HttpStatus.CONFLICT, "Selected slot is no longer available. Please choose another time slot");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only future slots can be booked");
        }
        if (bookingRepository.existsBySlotIdAndStatusIn(slot.getId(), ACTIVE_SLOT_STATUSES)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Selected slot has already been booked. Please choose another time slot");
        }
    }

    private TimeSlot getLockedSlot(Long slotId) {
        return timeSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Time slot not found"));
    }

    private void releaseSlot(TimeSlot slot) {
        slot.setStatus(slot.getStartTime().isAfter(LocalDateTime.now()) ? SlotStatus.AVAILABLE : SlotStatus.CLOSED);
        timeSlotRepository.save(slot);
    }

    private void ensureManager(UserAccount actor, Booking booking) {
        specialistService.ensureOwnerOrAdmin(actor, booking.getSpecialist());
    }

    private void ensureCustomerOwnsBooking(UserAccount actor, Booking booking) {
        if (!booking.getCustomer().getId().equals(actor.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Customer can only manage own bookings");
        }
    }

    private void ensureCanViewBooking(UserAccount actor, Booking booking) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.CUSTOMER && booking.getCustomer().getId().equals(actor.getId())) {
            return;
        }
        if (actor.getRole() == UserRole.SPECIALIST
                && booking.getSpecialist().getUser().getId().equals(actor.getId())) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "Current user cannot view this booking");
    }

    private BookingDtos.FeeBreakdownResponse mapFeeBreakdown(Booking booking) {
        PricingService.PriceCalculation calculation = pricingService.calculateBreakdown(
                booking.getUnitPrice(),
                booking.getSlot()
        );
        return new BookingDtos.FeeBreakdownResponse(
                booking.getId(),
                booking.getSpecialist().getUser().getFullName(),
                booking.getTopic(),
                booking.getSlot().getStartTime(),
                booking.getSlot().getEndTime(),
                Duration.between(booking.getSlot().getStartTime(), booking.getSlot().getEndTime()).toMinutes(),
                booking.getUnitPrice(),
                booking.getPricingMultiplier(),
                booking.getPrice(),
                BusinessConstants.DEFAULT_CURRENCY,
                mapFeeComponents(calculation)
        );
    }

    private List<BookingDtos.FeeSegmentResponse> mapFeeComponents(PricingService.PriceCalculation calculation) {
        return calculation.segments().stream()
                .map(segment -> new BookingDtos.FeeSegmentResponse(
                        segment.label(),
                        segment.startTime(),
                        segment.endTime(),
                        segment.durationMinutes(),
                        segment.multiplier(),
                        segment.amount()
                ))
                .toList();
    }

    private BookingDtos.BookingResponse mapBooking(Booking booking) {
        return new BookingDtos.BookingResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getCustomer().getFullName(),
                booking.getSpecialist().getId(),
                booking.getSpecialist().getUser().getFullName(),
                booking.getSlot().getId(),
                booking.getSlot().getStartTime(),
                booking.getSlot().getEndTime(),
                booking.getStatus(),
                booking.getTopic(),
                booking.getNotes(),
                booking.getPrice(),
                booking.getUnitPrice(),
                booking.getPricingMultiplier(),
                Duration.between(booking.getSlot().getStartTime(), booking.getSlot().getEndTime()).toMinutes(),
                BusinessConstants.DEFAULT_CURRENCY,
                booking.getLastActionReason(),
                booking.getCancelledAt(),
                evaluationRepository.existsByBookingId(booking.getId()),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

}
