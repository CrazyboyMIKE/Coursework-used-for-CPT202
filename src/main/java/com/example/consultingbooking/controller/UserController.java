package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.PageDtos;
import com.example.consultingbooking.dto.UserDtos;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.service.AuthService;
import com.example.consultingbooking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDtos.UserResponse me(@RequestHeader(AuthService.AUTH_HEADER) String token) {
        UserAccount user = authService.requireUser(token);
        return userService.currentUser(user);
    }

    @GetMapping
    public PageDtos.PageResponse<UserDtos.UserResponse> listUsers(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String keyword
    ) {
        UserAccount operator = authService.requireUser(token);
        return userService.listUsers(operator, keyword, pageRequest(page, size));
    }

    @PutMapping("/me")
    public UserDtos.UserResponse updateMe(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody UserDtos.UpdateProfileRequest request
    ) {
        UserAccount user = authService.requireUser(token);
        return userService.updateCurrentUser(user, request);
    }

    @PutMapping("/{id}")
    public UserDtos.UserResponse updateUser(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @PathVariable Long id,
            @Valid @RequestBody UserDtos.AdminUpdateUserRequest request
    ) {
        UserAccount operator = authService.requireUser(token);
        return userService.updateUser(operator, id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.UserResponse createUser(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody UserDtos.CreateUserRequest request
    ) {
        UserAccount operator = authService.requireUser(token);
        return userService.createUser(operator, request);
    }

    private PageRequest pageRequest(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "id"));
    }
}
