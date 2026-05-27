package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.PageDtos;
import com.example.consultingbooking.dto.SpecialistDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.SpecialistService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/specialists")
public class SpecialistController {

    private final SpecialistService specialistService;
    private final AuthService authService;

    public SpecialistController(SpecialistService specialistService, AuthService authService) {
        this.specialistService = specialistService;
        this.authService = authService;
    }

    @GetMapping
    public List<SpecialistDtos.SpecialistResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minFee,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableAt
    ) {
        return specialistService.searchSpecialists(categoryId, level, keyword, minFee, maxFee, availableAt);
    }

    @GetMapping("/{id}")
    public SpecialistDtos.SpecialistResponse detail(@PathVariable Long id) {
        return specialistService.specialistDetail(id);
    }

    @GetMapping("/manage")
    public PageDtos.PageResponse<SpecialistDtos.SpecialistResponse> manage(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String keyword
    ) {
        UserAccount operator = authService.requireUser(token);
        return specialistService.listSpecialistsForManagement(operator, keyword, PageRequestFactory.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        ));
    }

    @GetMapping("/me")
    public SpecialistDtos.SpecialistResponse me(@RequestHeader(AuthService.AUTH_HEADER) String token) {
        UserAccount actor = authService.requireUser(token);
        return specialistService.currentSpecialist(actor);
    }

    @PutMapping("/me")
    public SpecialistDtos.SpecialistResponse updateMe(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody SpecialistDtos.SpecialistSelfUpdateRequest request
    ) {
        UserAccount actor = authService.requireUser(token);
        return specialistService.updateCurrentSpecialist(actor, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialistDtos.SpecialistResponse create(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody SpecialistDtos.SpecialistRequest request
    ) {
        UserAccount operator = authService.requireUser(token);
        return specialistService.createSpecialist(operator, request);
    }

    @PutMapping("/{id}")
    public SpecialistDtos.SpecialistResponse update(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @Valid @RequestBody SpecialistDtos.SpecialistUpdateRequest request
    ) {
        UserAccount operator = authService.requireUser(token);
        return specialistService.updateSpecialist(operator, id, request);
    }

}
