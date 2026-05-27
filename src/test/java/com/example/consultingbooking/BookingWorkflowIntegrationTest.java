package com.example.consultingbooking;

import com.example.consultingbooking.dto.BookingDtos;
import com.example.consultingbooking.dto.AuthDtos;
import com.example.consultingbooking.dto.EvaluationDtos;
import com.example.consultingbooking.dto.ReportDtos;
import com.example.consultingbooking.dto.RefundDtos;
import com.example.consultingbooking.dto.SlotDtos;
import com.example.consultingbooking.dto.SpecialistDtos;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.NotificationType;
import com.example.consultingbooking.entity.RefundStatus;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.NotificationRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import com.example.consultingbooking.security.PasswordHasher;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.BookingService;
import com.example.consultingbooking.service.EvaluationService;
import com.example.consultingbooking.service.NotificationService;
import com.example.consultingbooking.service.ReportingService;
import com.example.consultingbooking.service.SlotService;
import com.example.consultingbooking.service.SpecialistService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingWorkflowIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ExpertiseCategoryRepository expertiseCategoryRepository;

    @Autowired
    private SpecialistService specialistService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SessionTokenRepository sessionTokenRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private SlotService slotService;

    @Test
    void shouldCreateBookingAndReserveSlot() {
        TestFixture fixture = createFixture();

        BookingDtos.BookingResponse booking = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(
                        fixture.specialist().getId(),
                        fixture.slot().getId(),
                        "Career planning",
                        "Need help with transition"
                )
        );

        Assertions.assertEquals(BookingStatus.PENDING, booking.status());
        Assertions.assertEquals(new BigDecimal("300.00"), booking.price());
        Assertions.assertEquals("USD", booking.feeCurrency());
        Assertions.assertEquals(SlotStatus.RESERVED, timeSlotRepository.findById(fixture.slot().getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldQuoteAndPersistSegmentedWeekendPriceFromTheSameCalculation() {
        TestFixture fixture = createFixture();
        TimeSlot crossRateSlot = createSlot(
                fixture.specialist(),
                LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)).atTime(23, 30)
        );

        BookingDtos.FeeQuoteResponse quote = bookingService.quoteBooking(
                fixture.customer(),
                new BookingDtos.QuoteRequest(fixture.specialist().getId(), crossRateSlot.getId())
        );
        BookingDtos.BookingResponse booking = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(
                        fixture.specialist().getId(),
                        crossRateSlot.getId(),
                        "Weekend transition advice",
                        null
                )
        );

        Assertions.assertEquals(new BigDecimal("322.50"), quote.totalPrice());
        Assertions.assertEquals(new BigDecimal("1.08"), quote.pricingMultiplier());
        Assertions.assertEquals(2, quote.components().size());
        Assertions.assertEquals(quote.totalPrice(), booking.price());
        Assertions.assertEquals(quote.pricingMultiplier(), booking.pricingMultiplier());
    }

    @Test
    void shouldKeepExistingBookingFeeWhenSpecialistChangesFutureRate() {
        TestFixture fixture = createFixture();
        BookingDtos.BookingResponse existingBooking = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(
                        fixture.specialist().getId(),
                        fixture.slot().getId(),
                        "Original agreed price",
                        null
                )
        );

        specialistService.updateCurrentSpecialist(
                fixture.specialist().getUser(),
                new SpecialistDtos.SpecialistSelfUpdateRequest(
                        fixture.specialist().getCategory().getId(),
                        fixture.specialist().getLevel(),
                        new BigDecimal("450.00"),
                        "USD",
                        fixture.specialist().getBio()
                )
        );
        TimeSlot laterSlot = createSlot(fixture.specialist(), nextWeekdayAt(16));
        BookingDtos.BookingResponse newBooking = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(
                        fixture.specialist().getId(),
                        laterSlot.getId(),
                        "New rate booking",
                        null
                )
        );

        BookingDtos.BookingResponse unchanged = bookingService.bookingDetails(fixture.customer(), existingBooking.id());
        Assertions.assertEquals(new BigDecimal("300.00"), unchanged.price());
        Assertions.assertEquals(new BigDecimal("300.00"), unchanged.unitPrice());
        Assertions.assertEquals(new BigDecimal("450.00"), newBooking.price());
        Assertions.assertEquals(new BigDecimal("450.00"), newBooking.unitPrice());
    }

    @Test
    void shouldPreventDoubleBookingForSameSlot() {
        TestFixture fixture = createFixture();
        UserAccount anotherCustomer = createUser("customer-b", UserRole.CUSTOMER);

        bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "Session A", null)
        );

        Assertions.assertThrows(BusinessException.class, () -> bookingService.createBooking(
                anotherCustomer,
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "Session B", null)
        ));
    }

    @Test
    void shouldRequireReconfirmationAfterReschedule() {
        TestFixture fixture = createFixture();
        TimeSlot nextSlot = createSlot(fixture.specialist(), nextWeekdayAt(12));

        BookingDtos.BookingResponse created = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "Initial", null)
        );
        BookingDtos.BookingResponse rescheduled = bookingService.rescheduleBooking(
                fixture.customer(),
                created.id(),
                new BookingDtos.RescheduleRequest(nextSlot.getId())
        );

        Assertions.assertEquals(BookingStatus.PENDING, rescheduled.status());
        Assertions.assertEquals(nextSlot.getId(), rescheduled.slotId());
        Assertions.assertEquals(SlotStatus.AVAILABLE, timeSlotRepository.findById(fixture.slot().getId()).orElseThrow().getStatus());
        Assertions.assertEquals(SlotStatus.RESERVED, timeSlotRepository.findById(nextSlot.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldCompleteConfirmedBookingAndCloseSlot() {
        TestFixture fixture = createFixture();
        BookingDtos.BookingResponse created = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "Closeout", null)
        );

        BookingDtos.BookingResponse confirmed = bookingService.confirmBooking(fixture.specialist().getUser(), created.id());
        BookingDtos.BookingResponse completed = bookingService.completeBooking(fixture.specialist().getUser(), confirmed.id());

        Assertions.assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        Assertions.assertEquals(BookingStatus.COMPLETED, completed.status());
        Assertions.assertEquals(SlotStatus.CLOSED, timeSlotRepository.findById(fixture.slot().getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRecordRejectedDecisionReasonSeparatelyFromCustomerCancellation() {
        TestFixture fixture = createFixture();
        BookingDtos.BookingResponse created = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "Declined request", null)
        );

        BookingDtos.BookingResponse rejected = bookingService.rejectBooking(
                fixture.specialist().getUser(),
                created.id(),
                "Outside my specialist area"
        );

        Assertions.assertEquals(BookingStatus.REJECTED, rejected.status());
        Assertions.assertEquals("Outside my specialist area", rejected.lastActionReason());
        Assertions.assertTrue(notificationService.listForUser(fixture.customer()).stream()
                .anyMatch(notification -> notification.type() == NotificationType.BOOKING_REJECTED
                        && notification.message().contains("Outside my specialist area")));
    }

    @Test
    void shouldRejectBookingWhenSlotBelongsToDifferentSpecialist() {
        TestFixture fixture = createFixture();
        UserAccount secondSpecialistUser = createUser("specialist-b", UserRole.SPECIALIST);
        UserAccount admin = createUser("admin-b", UserRole.ADMIN);
        ExpertiseCategory category = new ExpertiseCategory();
        category.setName("Operations");
        category.setDescription("Operations consulting");
        category.setActive(true);
        category = expertiseCategoryRepository.save(category);
        Long secondSpecialistId = specialistService.createSpecialist(
                admin,
                new com.example.consultingbooking.dto.SpecialistDtos.SpecialistRequest(
                        secondSpecialistUser.getId(),
                        category.getId(),
                        "Operations Consultant",
                        new BigDecimal("180.00"),
                        "USD",
                        SpecialistStatus.ACTIVE,
                        "Operations consultant"
                )
        ).id();
        TimeSlot secondSpecialistSlot = createSlot(specialistService.getEntity(secondSpecialistId), nextWeekdayAt(14));

        Assertions.assertThrows(BusinessException.class, () -> bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(
                        fixture.specialist().getId(),
                        secondSpecialistSlot.getId(),
                        "Mismatched slot",
                        null
                )
        ));

        Assertions.assertEquals(
                SlotStatus.AVAILABLE,
                timeSlotRepository.findById(secondSpecialistSlot.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldAllowLoginUsingUsernameEmailOrPhoneAfterRegistration() {
        AuthDtos.AuthResponse registered = authService.register(new AuthDtos.RegisterRequest(
                "new-customer",
                "password123",
                "New Customer",
                "new-customer@example.com",
                "18800001111"
        ));

        authService.logout(registered.token());

        AuthDtos.AuthResponse usernameLogin = authService.login(new AuthDtos.LoginRequest(
                "new-customer",
                "password123"
        ));
        AuthDtos.AuthResponse emailLogin = authService.login(new AuthDtos.LoginRequest(
                "new-customer@example.com",
                "password123"
        ));
        AuthDtos.AuthResponse phoneLogin = authService.login(new AuthDtos.LoginRequest(
                "18800001111",
                "password123"
        ));

        Assertions.assertEquals("new-customer", usernameLogin.username());
        Assertions.assertEquals("new-customer", emailLogin.username());
        Assertions.assertEquals("new-customer", phoneLogin.username());
        Assertions.assertFalse(usernameLogin.token().isBlank());
        Assertions.assertFalse(emailLogin.token().isBlank());
        Assertions.assertFalse(phoneLogin.token().isBlank());
        Assertions.assertEquals(3, sessionTokenRepository.findAll().size());

        String storedPassword = userAccountRepository.findByUsernameIgnoreCase("new-customer")
                .orElseThrow()
                .getPassword();
        Assertions.assertNotEquals("password123", storedPassword);
        Assertions.assertTrue(storedPassword.startsWith("sha256$" + PasswordHasher.SALT + "$"));
        Assertions.assertTrue(PasswordHasher.matches("password123", storedPassword));
    }

    @Test
    void shouldRejectRegistrationUsingAnExistingPhoneNumber() {
        authService.register(new AuthDtos.RegisterRequest(
                "first-phone-user",
                "password123",
                "First Phone User",
                "first-phone@example.com",
                "18800002222"
        ));

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> authService.register(
                new AuthDtos.RegisterRequest(
                        "second-phone-user",
                        "password123",
                        "Second Phone User",
                        "second-phone@example.com",
                        "18800002222"
                )
        ));

        Assertions.assertEquals("Phone number already exists", exception.getMessage());
    }

    @Test
    void shouldCreateFourRecurringWeeklySlotsWithoutOverwritingExistingAvailability() {
        TestFixture fixture = createFixture();
        DayOfWeek requestedDay = LocalDate.now().plusDays(2).getDayOfWeek();

        SlotDtos.RecurringSlotResponse response = slotService.createRecurringSlots(
                fixture.specialist().getUser(),
                fixture.specialist().getId(),
                new SlotDtos.RecurringSlotRequest(
                        requestedDay,
                        LocalTime.of(17, 0),
                        LocalTime.of(18, 0),
                        SlotDtos.ConflictPolicy.SKIP
                )
        );

        Assertions.assertEquals(4, response.requestedCount());
        Assertions.assertEquals(4, response.createdCount());
        Assertions.assertEquals(0, response.skippedCount());
        Assertions.assertEquals(4, response.createdSlots().size());
        Assertions.assertTrue(response.createdSlots().stream()
                .allMatch(slot -> slot.status() == SlotStatus.AVAILABLE));
    }

    @Test
    void shouldKeepFeeSnapshotNotifyCustomerAndAcceptOnlyOneCompletedEvaluation() {
        TestFixture fixture = createFixture();
        BookingDtos.BookingResponse created = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "Financial check-up", null)
        );
        bookingService.confirmBooking(fixture.specialist().getUser(), created.id());
        bookingService.completeBooking(fixture.specialist().getUser(), created.id());

        BookingDtos.FeeBreakdownResponse breakdown = bookingService.feeBreakdown(fixture.customer(), created.id());
        byte[] pdfDocument = bookingService.feeBreakdownPdf(fixture.customer(), created.id());
        Assertions.assertEquals(new BigDecimal("300.00"), breakdown.unitPrice());
        Assertions.assertEquals(new BigDecimal("300.00"), breakdown.totalPrice());
        Assertions.assertEquals(60, breakdown.durationMinutes());
        Assertions.assertTrue(new String(pdfDocument, 0, 8, StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
        Assertions.assertTrue(new String(pdfDocument, StandardCharsets.ISO_8859_1).endsWith("%%EOF\n"));

        Assertions.assertTrue(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(fixture.customer().getId())
                .stream()
                .anyMatch(notification -> notification.getType() == NotificationType.BOOKING_CONFIRMED));

        EvaluationDtos.EvaluationResponse evaluation = evaluationService.submit(
                fixture.customer(),
                created.id(),
                new EvaluationDtos.SubmitEvaluationRequest(5, "Clear and helpful consultation.")
        );
        Assertions.assertEquals(5, evaluation.rating());
        Assertions.assertThrows(BusinessException.class, () -> evaluationService.submit(
                fixture.customer(),
                created.id(),
                new EvaluationDtos.SubmitEvaluationRequest(4, "Trying to submit twice.")
        ));

        LocalDate appointmentDate = fixture.slot().getStartTime().toLocalDate();
        ReportDtos.EarningsResponse earnings = reportingService.myEarnings(
                fixture.specialist().getUser(),
                appointmentDate.minusDays(1),
                appointmentDate.plusDays(1)
        );
        Assertions.assertEquals(new BigDecimal("300.00"), earnings.totalEarnings());
        Assertions.assertEquals(1, earnings.entries().size());
        Assertions.assertEquals(60, earnings.entries().getFirst().durationMinutes());
        Assertions.assertEquals(new BigDecimal("300.00"), earnings.entries().getFirst().unitPrice());
    }

    @Test
    void shouldFilterCompletedConsultationsByDateAndProtectAppointmentDetails() {
        TestFixture fixture = createFixture();
        BookingDtos.BookingResponse first = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), fixture.slot().getId(), "First completed consultation", null)
        );
        bookingService.confirmBooking(fixture.specialist().getUser(), first.id());
        bookingService.completeBooking(fixture.specialist().getUser(), first.id());

        TimeSlot laterSlot = createSlot(fixture.specialist(), fixture.slot().getStartTime().plusDays(10));
        BookingDtos.BookingResponse later = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), laterSlot.getId(), "Later completed consultation", null)
        );
        bookingService.confirmBooking(fixture.specialist().getUser(), later.id());
        bookingService.completeBooking(fixture.specialist().getUser(), later.id());

        LocalDate firstDate = fixture.slot().getStartTime().toLocalDate();
        java.util.List<BookingDtos.BookingResponse> filtered = bookingService.specialistSchedule(
                fixture.specialist().getUser(),
                BookingStatus.COMPLETED,
                firstDate,
                firstDate
        );
        BookingDtos.BookingResponse detail = bookingService.bookingDetails(fixture.specialist().getUser(), first.id());
        UserAccount unrelatedCustomer = createUser("unrelated-customer", UserRole.CUSTOMER);

        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals(first.id(), filtered.getFirst().id());
        Assertions.assertEquals("First completed consultation", detail.topic());
        Assertions.assertThrows(BusinessException.class, () -> bookingService.bookingDetails(unrelatedCustomer, first.id()));
    }

    @Test
    void shouldCreateReminderWithinTwentyFourHoursAndRecordCancellationTime() {
        TestFixture fixture = createFixture();
        TimeSlot nearSlot = createSlot(fixture.specialist(), LocalDateTime.now().plusHours(3));
        BookingDtos.BookingResponse created = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), nearSlot.getId(), "Near appointment", null)
        );
        bookingService.confirmBooking(fixture.specialist().getUser(), created.id());

        notificationService.generateDueAppointmentReminders();
        com.example.consultingbooking.dto.NotificationDtos.NotificationResponse reminder = notificationService
                .listForUser(fixture.customer()).stream()
                .filter(notification -> notification.type() == NotificationType.APPOINTMENT_REMINDER)
                .findFirst()
                .orElseThrow();
        Assertions.assertFalse(reminder.read());
        Assertions.assertTrue(notificationService.markRead(fixture.customer(), reminder.id()).read());

        BookingDtos.BookingResponse cancelled = bookingService.cancelBooking(
                fixture.specialist().getUser(),
                created.id(),
                "Specialist unavailable"
        );
        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.status());
        Assertions.assertNotNull(cancelled.cancelledAt());
        RefundDtos.RefundResponse refund = bookingService.refundDetails(fixture.customer(), cancelled.id());
        Assertions.assertEquals(RefundStatus.NOT_REQUIRED, refund.status());
        Assertions.assertEquals(new BigDecimal("0.00"), refund.amount());
        Assertions.assertTrue(refund.policyMessage().contains("no transfer required"));
    }

    @Test
    void shouldSynchroniseRefundOutcomeForEligibleCustomerCancellation() {
        TestFixture fixture = createFixture();
        TimeSlot futureSlot = createSlot(fixture.specialist(), LocalDateTime.now().plusDays(3));
        BookingDtos.BookingResponse created = bookingService.createBooking(
                fixture.customer(),
                new BookingDtos.CreateBookingRequest(fixture.specialist().getId(), futureSlot.getId(), "Cancel in advance", null)
        );
        bookingService.confirmBooking(fixture.specialist().getUser(), created.id());

        BookingDtos.BookingResponse cancelled = bookingService.cancelBooking(
                fixture.customer(),
                created.id(),
                "Schedule changed"
        );
        RefundDtos.RefundResponse refund = bookingService.refundDetails(fixture.customer(), cancelled.id());

        Assertions.assertEquals(BookingStatus.CANCELLED, cancelled.status());
        Assertions.assertEquals(RefundStatus.NOT_REQUIRED, refund.status());
        Assertions.assertEquals(created.id(), refund.bookingId());
        Assertions.assertThrows(BusinessException.class, () -> bookingService.cancelBooking(
                fixture.customer(),
                created.id(),
                "Second cancellation"
        ));
    }

    private TestFixture createFixture() {
        UserAccount customer = createUser("customer-a", UserRole.CUSTOMER);
        UserAccount specialistUser = createUser("specialist-a", UserRole.SPECIALIST);
        UserAccount admin = createUser("admin-a", UserRole.ADMIN);
        ExpertiseCategory category = new ExpertiseCategory();
        category.setName("Strategy");
        category.setDescription("Strategy consulting");
        category.setActive(true);
        category = expertiseCategoryRepository.save(category);

        Long specialistId = specialistService.createSpecialist(
                admin,
                new com.example.consultingbooking.dto.SpecialistDtos.SpecialistRequest(
                        specialistUser.getId(),
                        category.getId(),
                        "Senior Strategy Consultant",
                        new BigDecimal("300.00"),
                        "USD",
                        SpecialistStatus.ACTIVE,
                        "Senior strategy consultant"
                )
        ).id();

        TimeSlot slot = createSlot(
                specialistService.getEntity(specialistId),
                nextWeekdayAt(10)
        );
        return new TestFixture(customer, specialistService.getEntity(specialistId), slot);
    }

    private UserAccount createUser(String username, UserRole role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(PasswordHasher.hash("password123"));
        user.setFullName(username);
        user.setEmail(username + "@example.com");
        user.setPhone(null);
        user.setRole(role);
        user.setActive(true);
        return userAccountRepository.save(user);
    }

    private TimeSlot createSlot(SpecialistProfile specialist, LocalDateTime startTime) {
        TimeSlot slot = new TimeSlot();
        slot.setSpecialist(specialist);
        slot.setStartTime(startTime.withSecond(0).withNano(0));
        slot.setEndTime(startTime.withSecond(0).withNano(0).plusHours(1));
        slot.setStatus(SlotStatus.AVAILABLE);
        return timeSlotRepository.save(slot);
    }

    private LocalDateTime nextWeekdayAt(int hour) {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(1).withHour(hour).withMinute(0).withSecond(0).withNano(0);
        while (dateTime.getDayOfWeek() == DayOfWeek.SATURDAY || dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dateTime = dateTime.plusDays(1);
        }
        return dateTime;
    }

    private record TestFixture(UserAccount customer, SpecialistProfile specialist, TimeSlot slot) {
    }
}
