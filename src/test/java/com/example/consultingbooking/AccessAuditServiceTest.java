package com.example.consultingbooking;

import com.example.consultingbooking.entity.AccessAuditLog;
import com.example.consultingbooking.entity.SessionToken;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.repository.AccessAuditLogRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import com.example.consultingbooking.service.AccessAuditService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessAuditServiceTest {

    @Test
    void deniedAccessRecordsActorAndRequestedPathWithoutPersistingToken() {
        AccessAuditLogRepository auditRepository = Mockito.mock(AccessAuditLogRepository.class);
        SessionTokenRepository tokenRepository = Mockito.mock(SessionTokenRepository.class);
        AccessAuditService service = new AccessAuditService(auditRepository, tokenRepository);
        UserAccount user = new UserAccount();
        user.setId(9L);
        SessionToken token = new SessionToken();
        token.setUser(user);
        Mockito.when(tokenRepository.findByTokenAndActiveTrue("session-token")).thenReturn(Optional.of(token));

        service.recordDeniedAccess("GET", "/api/reports/summary", HttpStatus.FORBIDDEN, "Access denied", "session-token");

        ArgumentCaptor<AccessAuditLog> captor = ArgumentCaptor.forClass(AccessAuditLog.class);
        Mockito.verify(auditRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
        assertEquals("/api/reports/summary", captor.getValue().getRequestPath());
        assertEquals(403, captor.getValue().getResponseStatus());
    }
}
