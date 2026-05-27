package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.EvaluationDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.EvaluationService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final AuthService authService;

    public EvaluationController(EvaluationService evaluationService, AuthService authService) {
        this.evaluationService = evaluationService;
        this.authService = authService;
    }

    @PostMapping("/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationDtos.EvaluationResponse submit(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long bookingId,
            @Valid @RequestBody EvaluationDtos.SubmitEvaluationRequest request
    ) {
        UserAccount customer = authService.requireUser(token);
        return evaluationService.submit(customer, bookingId, request);
    }

    @GetMapping("/bookings/{bookingId}")
    public EvaluationDtos.EvaluationResponse find(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long bookingId
    ) {
        UserAccount customer = authService.requireUser(token);
        return evaluationService.findForBooking(customer, bookingId);
    }
}
