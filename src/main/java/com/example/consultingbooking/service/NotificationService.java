package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.NotificationDtos;
import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.Notification;
import com.example.consultingbooking.entity.NotificationType;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.BookingRepository;
import com.example.consultingbooking.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;

    public NotificationService(NotificationRepository notificationRepository, BookingRepository bookingRepository) {
        this.notificationRepository = notificationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public List<NotificationDtos.NotificationResponse> listForUser(UserAccount user) {
        createDueReminders(user);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapNotification)
                .toList();
    }

    @Scheduled(fixedDelayString = "${booking.reminder.scan-delay-ms:60000}")
    @Transactional
    public void generateDueAppointmentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(24);
        bookingRepository
                .findByStatusAndSlotStartTimeGreaterThanAndSlotStartTimeLessThanEqualOrderBySlotStartTimeAsc(
                        BookingStatus.CONFIRMED,
                        now,
                        reminderWindowEnd
                )
                .forEach(this::createReminderIfMissing);
    }

    @Transactional
    public NotificationDtos.NotificationResponse markRead(UserAccount user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Users can only read their own notifications");
        }
        notification.setRead(true);
        return mapNotification(notificationRepository.save(notification));
    }

    @Transactional
    public void notifyConfirmed(Booking booking) {
        createNotification(
                booking.getCustomer(),
                booking,
                NotificationType.BOOKING_CONFIRMED,
                "Booking confirmed",
                "Your consultation \"" + booking.getTopic() + "\" has been confirmed."
        );
    }

    @Transactional
    public void notifyRejected(Booking booking) {
        createNotification(
                booking.getCustomer(),
                booking,
                NotificationType.BOOKING_REJECTED,
                "Booking declined",
                "Your consultation \"" + booking.getTopic() + "\" was declined. "
                        + booking.getLastActionReason()
        );
    }

    @Transactional
    public void notifyCancelled(Booking booking, UserAccount actor) {
        UserAccount recipient = actor.getRole() == UserRole.CUSTOMER
                ? booking.getSpecialist().getUser()
                : booking.getCustomer();
        createNotification(
                recipient,
                booking,
                NotificationType.BOOKING_CANCELLED,
                "Booking cancelled",
                "The consultation \"" + booking.getTopic() + "\" has been cancelled."
        );
    }

    @Transactional
    public void notifyRescheduled(Booking booking) {
        createNotification(
                booking.getSpecialist().getUser(),
                booking,
                NotificationType.BOOKING_RESCHEDULED,
                "Booking rescheduled",
                "A customer rescheduled \"" + booking.getTopic() + "\" and it requires confirmation."
        );
    }

    private void createDueReminders(UserAccount user) {
        if (user.getRole() != UserRole.CUSTOMER) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusHours(24);
        bookingRepository.findByCustomerIdAndStatusOrderBySlotStartTimeDesc(user.getId(), BookingStatus.CONFIRMED)
                .stream()
                .filter(booking -> booking.getSlot().getStartTime().isAfter(now))
                .filter(booking -> !booking.getSlot().getStartTime().isAfter(reminderWindowEnd))
                .forEach(this::createReminderIfMissing);
    }

    private void createReminderIfMissing(Booking booking) {
        if (notificationRepository.existsByRecipientIdAndBookingIdAndType(
                booking.getCustomer().getId(),
                booking.getId(),
                NotificationType.APPOINTMENT_REMINDER
        )) {
            return;
        }
        createNotification(
                booking.getCustomer(),
                booking,
                NotificationType.APPOINTMENT_REMINDER,
                "Appointment reminder",
                "Your consultation \"" + booking.getTopic() + "\" starts within 24 hours."
        );
    }

    private void createNotification(
            UserAccount recipient,
            Booking booking,
            NotificationType type,
            String title,
            String message
    ) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setBooking(booking);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    private NotificationDtos.NotificationResponse mapNotification(Notification notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getId(),
                notification.getBooking() == null ? null : notification.getBooking().getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
