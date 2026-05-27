package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.NotificationDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public List<NotificationDtos.NotificationResponse> me(@RequestHeader(AuthService.AUTH_HEADER) String token) {
        UserAccount user = authService.requireUser(token);
        return notificationService.listForUser(user);
    }

    @PostMapping("/{id}/read")
    public NotificationDtos.NotificationResponse read(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id
    ) {
        UserAccount user = authService.requireUser(token);
        return notificationService.markRead(user, id);
    }
}
