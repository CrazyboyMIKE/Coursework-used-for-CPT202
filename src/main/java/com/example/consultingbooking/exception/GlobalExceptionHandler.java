package com.example.consultingbooking.exception;

import com.example.consultingbooking.dto.ApiErrorResponse;
import com.example.consultingbooking.service.AccessAuditService;
import com.example.consultingbooking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String INVALID_REQUEST_PARAMETER = "INVALID_REQUEST_PARAMETER";
    private static final String INVALID_REQUEST_BODY = "INVALID_REQUEST_BODY";
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private final AccessAuditService accessAuditService;

    public GlobalExceptionHandler(AccessAuditService accessAuditService) {
        this.accessAuditService = accessAuditService;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        if (ex.getStatus() == HttpStatus.UNAUTHORIZED || ex.getStatus() == HttpStatus.FORBIDDEN) {
            try {
                accessAuditService.recordDeniedAccess(
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getStatus(),
                        ex.getMessage(),
                        request.getHeader(AuthService.AUTH_HEADER)
                );
            } catch (RuntimeException auditFailure) {
                log.warn("Unable to write denied-access audit record", auditFailure);
            }
        }
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), Collections.emptyMap());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, message, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, ex.getMessage(), Collections.emptyMap());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String fieldName = ex.getName();
        String message = fieldName + ": invalid value";
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(fieldName, "Invalid value");
        return buildResponse(HttpStatus.BAD_REQUEST, INVALID_REQUEST_PARAMETER, message, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST_BODY,
                "Request body is invalid or malformed",
                Collections.emptyMap()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingResource(NoResourceFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                RESOURCE_NOT_FOUND,
                "Resource not found",
                Collections.emptyMap()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex) {
        log.error("Unexpected server error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Unexpected server error", Collections.emptyMap());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                fieldErrors
        ));
    }
}
