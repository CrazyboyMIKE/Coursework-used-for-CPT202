package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.BookingDtos;
import com.example.consultingbooking.dto.PageDtos;
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
import com.example.consultingbooking.repository.TimeSlotRepository;
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
    private final SpecialistService specialistService;
    private final SlotService slotService;
    private final PricingService pricingService;
    private final AuthService authService;

    public BookingService(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            SpecialistService specialistService,
            SlotService slotService,
            PricingService pricingService,
            AuthService authService
    ) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.specialistService = specialistService;
        this.slotService = slotService;
        this.pricingService = pricingService;
        this.authService = authService;
    }

    @Transactional
    public BookingDtos.BookingResponse createBooking(UserAccount customer, BookingDtos.CreateBookingRequest request) {
        authService.ensureRole(customer, UserRole.CUSTOMER);

        SpecialistProfile specialist = specialistService.getEntity(request.specialistId());
        TimeSlot slot = slotService.getEntity(request.slotId());
        ensureBookableSpecialist(specialist);
        ensureSlotMatchesSpecialist(slot, specialist);
        ensureSlotAvailable(slot);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSpecialist(specialist);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTopic(request.topic().trim());
        booking.setNotes(request.notes());
        booking.setPrice(pricingService.calculatePrice(specialist, slot));
        booking.setLastActionReason("Booking created");

        slot.setStatus(SlotStatus.RESERVED);
        timeSlotRepository.save(slot);
        return mapBooking(bookingRepository.save(booking));
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
        return mapBooking(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDtos.BookingResponse rejectBooking(UserAccount actor, Long bookingId, String reason) {
        Booking booking = getEntity(bookingId);
        ensureManager(actor, booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only pending bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setLastActionReason(reason == null || reason.isBlank() ? "Booking rejected" : reason.trim());
        releaseSlot(booking.getSlot());
        return mapBooking(bookingRepository.save(booking));
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
        releaseSlot(booking.getSlot());
        return mapBooking(bookingRepository.save(booking));
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

        TimeSlot newSlot = slotService.getEntity(request.newSlotId());
        if (newSlot.getId().equals(booking.getSlot().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "New slot must be different from current slot");
        }
        ensureBookableSpecialist(newSlot.getSpecialist());
        ensureSlotAvailable(newSlot);

        releaseSlot(booking.getSlot());
        newSlot.setStatus(SlotStatus.RESERVED);
        timeSlotRepository.save(newSlot);

        booking.setSpecialist(newSlot.getSpecialist());
        booking.setSlot(newSlot);
        booking.setPrice(pricingService.calculatePrice(newSlot.getSpecialist(), newSlot));
        booking.setStatus(BookingStatus.PENDING);
        booking.setLastActionReason("Booking rescheduled and requires reconfirmation");
        return mapBooking(bookingRepository.save(booking));
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
        authService.ensureRole(specialistUser, UserRole.SPECIALIST);
        List<Booking> bookings = status == null
                ? bookingRepository.findBySpecialistUserIdOrderBySlotStartTimeAsc(specialistUser.getId())
                : bookingRepository.findBySpecialistUserIdAndStatusOrderBySlotStartTimeAsc(specialistUser.getId(), status);
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
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selected slot is not available");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only future slots can be booked");
        }
        if (bookingRepository.existsBySlotIdAndStatusIn(slot.getId(), ACTIVE_SLOT_STATUSES)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Slot is already booked");
        }
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
                BusinessConstants.DEFAULT_CURRENCY,
                booking.getLastActionReason(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

}
