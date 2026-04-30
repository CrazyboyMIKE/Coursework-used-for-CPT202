package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
