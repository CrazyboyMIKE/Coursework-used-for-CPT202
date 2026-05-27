package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsBySlotIdAndStatusIn(Long slotId, Collection<BookingStatus> statuses);

    boolean existsByTopic(String topic);

    Optional<Booking> findByTopic(String topic);

    List<Booking> findByCustomerIdOrderBySlotStartTimeDesc(Long customerId);

    List<Booking> findByCustomerIdAndStatusOrderBySlotStartTimeDesc(Long customerId, BookingStatus status);

    List<Booking> findByStatusAndSlotStartTimeGreaterThanAndSlotStartTimeLessThanEqualOrderBySlotStartTimeAsc(
            BookingStatus status,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    );

    List<Booking> findBySpecialistUserIdOrderBySlotStartTimeAsc(Long userId);

    List<Booking> findBySpecialistUserIdAndStatusOrderBySlotStartTimeAsc(Long userId, BookingStatus status);

    @Query("""
            select b from Booking b
            join b.slot slot
            where b.specialist.user.id = :userId
              and (:status is null or b.status = :status)
              and (:rangeStart is null or slot.startTime >= :rangeStart)
              and (:rangeEnd is null or slot.startTime < :rangeEnd)
            order by slot.startTime asc
            """)
    List<Booking> findSpecialistSchedule(
            @Param("userId") Long userId,
            @Param("status") BookingStatus status,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    List<Booking> findBySpecialistUserIdAndStatusAndSlotStartTimeGreaterThanEqualAndSlotStartTimeLessThanOrderBySlotStartTimeDesc(
            Long userId,
            BookingStatus status,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    );

    List<Booking> findAllByOrderBySlotStartTimeDesc();

    List<Booking> findByStatusOrderBySlotStartTimeDesc(BookingStatus status);

    @Query("select coalesce(sum(b.price), 0) from Booking b where b.status = 'CONFIRMED' or b.status = 'COMPLETED'")
    BigDecimal totalConfirmedRevenue();

    @Query(
            value = """
                    select b from Booking b
                    join b.customer customer
                    join b.specialist specialist
                    join specialist.user specialistUser
                    join b.slot slot
                    where (:status is null or b.status = :status)
                      and (
                          :keyword is null
                          or lower(b.topic) like concat('%', :keyword, '%')
                          or lower(coalesce(b.notes, '')) like concat('%', :keyword, '%')
                          or lower(coalesce(b.lastActionReason, '')) like concat('%', :keyword, '%')
                          or lower(customer.fullName) like concat('%', :keyword, '%')
                          or lower(customer.username) like concat('%', :keyword, '%')
                          or lower(specialistUser.fullName) like concat('%', :keyword, '%')
                          or lower(specialistUser.username) like concat('%', :keyword, '%')
                          or str(b.id) like concat('%', :keyword, '%')
                      )
                    order by slot.startTime desc
                    """,
            countQuery = """
                    select count(b) from Booking b
                    join b.customer customer
                    join b.specialist specialist
                    join specialist.user specialistUser
                    where (:status is null or b.status = :status)
                      and (
                          :keyword is null
                          or lower(b.topic) like concat('%', :keyword, '%')
                          or lower(coalesce(b.notes, '')) like concat('%', :keyword, '%')
                          or lower(coalesce(b.lastActionReason, '')) like concat('%', :keyword, '%')
                          or lower(customer.fullName) like concat('%', :keyword, '%')
                          or lower(customer.username) like concat('%', :keyword, '%')
                          or lower(specialistUser.fullName) like concat('%', :keyword, '%')
                          or lower(specialistUser.username) like concat('%', :keyword, '%')
                          or str(b.id) like concat('%', :keyword, '%')
                      )
                    """
    )
    Page<Booking> searchForAdmin(
            @Param("status") BookingStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
