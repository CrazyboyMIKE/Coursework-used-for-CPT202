package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.SlotDtos;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.SlotService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService slotService;
    private final AuthService authService;

    public SlotController(SlotService slotService, AuthService authService) {
        this.slotService = slotService;
        this.authService = authService;
    }

    @GetMapping("/specialists/{specialistId}")
    public List<SlotDtos.SlotResponse> listSlots(
            @PathVariable Long specialistId,
            @RequestParam(required = false) SlotStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) Integer days
    ) {
        return slotService.listSlots(specialistId, status, fromDate, days);
    }

    @PostMapping("/specialists/{specialistId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotDtos.SlotResponse createSlot(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long specialistId,
            @Valid @RequestBody SlotDtos.SlotRequest request
    ) {
        UserAccount actor = authService.requireUser(token);
        return slotService.createSlot(actor, specialistId, request);
    }
}
