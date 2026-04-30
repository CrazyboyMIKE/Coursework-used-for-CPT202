package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.ReportDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.ReportingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
