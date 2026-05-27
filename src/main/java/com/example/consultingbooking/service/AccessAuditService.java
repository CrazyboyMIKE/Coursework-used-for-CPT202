package com.example.consultingbooking.service;

import com.example.consultingbooking.entity.AccessAuditLog;
import com.example.consultingbooking.repository.AccessAuditLogRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessAuditService {

    private final AccessAuditLogRepository accessAuditLogRepository;
    private final SessionTokenRepository sessionTokenRepository;

    public AccessAuditService(
            AccessAuditLogRepository accessAuditLogRepository,
            SessionTokenRepository sessionTokenRepository
    ) {
        this.accessAuditLogRepository = accessAuditLogRepository;
        this.sessionTokenRepository = sessionTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDeniedAccess(
            String method,
            String path,
            HttpStatus status,
            String reason,
            String token
    ) {
        AccessAuditLog auditLog = new AccessAuditLog();
        auditLog.setRequestMethod(method);
        auditLog.setRequestPath(path);
        auditLog.setResponseStatus(status.value());
        auditLog.setReason(reason);

        if (token != null && !token.isBlank()) {
            sessionTokenRepository.findByTokenAndActiveTrue(token)
                    .ifPresent(session -> auditLog.setUser(session.getUser()));
        }

        accessAuditLogRepository.save(auditLog);
    }
}
