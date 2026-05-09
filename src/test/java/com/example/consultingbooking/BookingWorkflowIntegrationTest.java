package com.example.consultingbooking;

import com.example.consultingbooking.dto.BookingDtos;
import com.example.consultingbooking.dto.AuthDtos;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import com.example.consultingbooking.security.PasswordHasher;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.BookingService;
import com.example.consultingbooking.service.SpecialistService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
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
    void shouldAllowLoginUsingUsernameOrEmailAfterRegistration() {
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

        Assertions.assertEquals("new-customer", usernameLogin.username());
        Assertions.assertEquals("new-customer", emailLogin.username());
        Assertions.assertFalse(usernameLogin.token().isBlank());
        Assertions.assertFalse(emailLogin.token().isBlank());
        Assertions.assertEquals(2, sessionTokenRepository.findAll().size());

        String storedPassword = userAccountRepository.findByUsernameIgnoreCase("new-customer")
                .orElseThrow()
                .getPassword();
        Assertions.assertNotEquals("password123", storedPassword);
        Assertions.assertTrue(storedPassword.startsWith("sha256$" + PasswordHasher.SALT + "$"));
        Assertions.assertTrue(PasswordHasher.matches("password123", storedPassword));
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
        user.setPhone("18800000000");
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
