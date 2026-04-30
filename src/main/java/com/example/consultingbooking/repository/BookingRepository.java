package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.BookingStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsBySlotIdAndStatusIn(Long slotId, Collection<BookingStatus> statuses);

    List<Booking> findByCustomerIdOrderBySlotStartTimeDesc(Long customerId);

    List<Booking> findByCustomerIdAndStatusOrderBySlotStartTimeDesc(Long customerId, BookingStatus status);

    List<Booking> findBySpecialistUserIdOrderBySlotStartTimeAsc(Long userId);

    List<Booking> findBySpecialistUserIdAndStatusOrderBySlotStartTimeAsc(Long userId, BookingStatus status);

    @Query("select coalesce(sum(b.price), 0) from Booking b where b.status = 'CONFIRMED' or b.status = 'COMPLETED'")
    BigDecimal totalConfirmedRevenue();
}
