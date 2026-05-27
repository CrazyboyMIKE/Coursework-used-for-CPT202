package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.AccessAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, Long> {

    void deleteByUserId(Long userId);
}
