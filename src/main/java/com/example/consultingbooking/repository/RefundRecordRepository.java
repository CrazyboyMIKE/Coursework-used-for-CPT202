package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.RefundRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {

    Optional<RefundRecord> findByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
