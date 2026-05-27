package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.ReportDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.ReportingService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;
    private final AuthService authService;

    public ReportingController(ReportingService reportingService, AuthService authService) {
        this.reportingService = reportingService;
        this.authService = authService;
    }

    @GetMapping("/summary")
    public ReportDtos.SummaryResponse summary(@RequestHeader(AuthService.AUTH_HEADER) String token) {
        UserAccount operator = authService.requireUser(token);
        return reportingService.summary(operator);
    }

    @GetMapping("/my-earnings")
    public ReportDtos.EarningsResponse myEarnings(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        UserAccount specialist = authService.requireUser(token);
        return reportingService.myEarnings(specialist, fromDate, toDate);
    }
}
