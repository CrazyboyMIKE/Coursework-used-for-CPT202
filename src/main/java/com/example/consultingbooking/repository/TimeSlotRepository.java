package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.TimeSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findBySpecialistIdOrderByStartTimeAsc(Long specialistId);

    List<TimeSlot> findBySpecialistIdAndStatusOrderByStartTimeAsc(Long specialistId, SlotStatus status);

    List<TimeSlot> findBySpecialistIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Long specialistId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    List<TimeSlot> findBySpecialistIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Long specialistId,
            SlotStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    boolean existsBySpecialistIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long specialistId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    boolean existsBySpecialistIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long specialistId,
            Long slotId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<TimeSlot> findBySpecialistIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
            Long specialistId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from TimeSlot slot where slot.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);
}
