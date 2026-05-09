package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.BookingDtos;
import com.example.consultingbooking.dto.PageDtos;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final AuthService authService;

    public BookingController(BookingService bookingService, AuthService authService) {
        this.bookingService = bookingService;
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDtos.BookingResponse create(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody BookingDtos.CreateBookingRequest request
    ) {
        UserAccount customer = authService.requireUser(token);
        return bookingService.createBooking(customer, request);
    }

    @PostMapping("/{id}/confirm")
    public BookingDtos.BookingResponse confirm(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.confirmBooking(actor, id);
    }

    @PostMapping("/{id}/reject")
    public BookingDtos.BookingResponse reject(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @RequestBody(required = false) BookingDtos.ReasonRequest request
    ) {
        UserAccount actor = authService.requireUser(token);
        String reason = request == null ? null : request.reason();
        return bookingService.rejectBooking(actor, id, reason);
    }

    @PostMapping("/{id}/cancel")
    public BookingDtos.BookingResponse cancel(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @RequestBody(required = false) BookingDtos.ReasonRequest request
    ) {
        UserAccount actor = authService.requireUser(token);
        String reason = request == null ? null : request.reason();
        return bookingService.cancelBooking(actor, id, reason);
    }

    @PostMapping("/{id}/reschedule")
    public BookingDtos.BookingResponse reschedule(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @Valid @RequestBody BookingDtos.RescheduleRequest request
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.rescheduleBooking(actor, id, request);
    }

    @PostMapping("/{id}/complete")
    public BookingDtos.BookingResponse complete(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.completeBooking(actor, id);
    }

    @GetMapping("/me")
    public List<BookingDtos.BookingResponse> myBookings(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BookingStatus status
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.customerBookings(actor, status);
    }

    @GetMapping("/schedule")
    public List<BookingDtos.BookingResponse> schedule(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BookingStatus status
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.specialistSchedule(actor, status);
    }

    @GetMapping("/manage")
    public PageDtos.PageResponse<BookingDtos.BookingResponse> manage(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BookingStatus status,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "8") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.adminBookings(actor, status, keyword, pageRequest(page, size));
    }

    private PageRequest pageRequest(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
