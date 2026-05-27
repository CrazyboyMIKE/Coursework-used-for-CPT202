package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.Notification;
import com.example.consultingbooking.entity.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    boolean existsByRecipientIdAndBookingIdAndType(Long recipientId, Long bookingId, NotificationType type);

    void deleteByBookingId(Long bookingId);
}
