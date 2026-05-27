package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.BookingDtos;
import com.example.consultingbooking.dto.PageDtos;
import com.example.consultingbooking.dto.RefundDtos;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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

    @PostMapping("/quote")
    public BookingDtos.FeeQuoteResponse quote(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody BookingDtos.QuoteRequest request
    ) {
        UserAccount customer = authService.requireUser(token);
        return bookingService.quoteBooking(customer, request);
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
            @Valid @RequestBody BookingDtos.RejectRequest request
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.rejectBooking(actor, id, request.reason());
    }

    @PostMapping("/{id}/cancel")
    public BookingDtos.BookingResponse cancel(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BookingDtos.ReasonRequest request
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
            @org.springframework.web.bind.annotation.RequestParam(required = false) BookingStatus status,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.specialistSchedule(actor, status, fromDate, toDate);
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
        return bookingService.adminBookings(actor, status, keyword, PageRequestFactory.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        ));
    }

    @GetMapping("/{id}/fee-breakdown")
    public BookingDtos.FeeBreakdownResponse feeBreakdown(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.feeBreakdown(actor, id);
    }

    @GetMapping("/details/{id}")
    public BookingDtos.BookingResponse details(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.bookingDetails(actor, id);
    }

    @GetMapping(value = "/{id}/fee-breakdown.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadFeeBreakdown(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount actor = authService.requireUser(token);
        byte[] document = bookingService.feeBreakdownPdf(actor, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=booking-" + id + "-fee-breakdown.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(document);
    }

    @GetMapping("/{id}/refund")
    public RefundDtos.RefundResponse refund(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount actor = authService.requireUser(token);
        return bookingService.refundDetails(actor, id);
    }

}
