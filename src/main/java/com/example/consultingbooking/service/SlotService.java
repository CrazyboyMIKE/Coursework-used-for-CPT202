package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.SlotDtos;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.TimeSlotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final SpecialistService specialistService;

    public SlotService(TimeSlotRepository timeSlotRepository, SpecialistService specialistService) {
        this.timeSlotRepository = timeSlotRepository;
        this.specialistService = specialistService;
    }

    @Transactional
    public SlotDtos.SlotResponse createSlot(UserAccount actor, Long specialistId, SlotDtos.SlotRequest request) {
        validateSlotTime(request.startTime(), request.endTime());

        SpecialistProfile specialist = specialistService.getEntity(specialistId);
        specialistService.ensureOwnerOrAdmin(actor, specialist);

        if (timeSlotRepository.existsBySpecialistIdAndStartTimeLessThanAndEndTimeGreaterThan(
                specialistId,
                request.endTime(),
                request.startTime()
        )) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Slot overlaps with an existing slot");
        }

        TimeSlot slot = new TimeSlot();
        slot.setSpecialist(specialist);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setStatus(SlotStatus.AVAILABLE);
        return mapSlot(timeSlotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public List<SlotDtos.SlotResponse> listSlots(Long specialistId, SlotStatus status, LocalDate fromDate, Integer days) {
        List<TimeSlot> slots;
        if (fromDate != null) {
            int windowDays = days == null ? 7 : days;
            if (windowDays < 1 || windowDays > 31) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "The day range must be between 1 and 31");
            }

            LocalDateTime rangeStart = fromDate.atStartOfDay();
            LocalDateTime rangeEnd = fromDate.plusDays(windowDays).atStartOfDay();
            slots = status == null
                    ? timeSlotRepository.findBySpecialistIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                            specialistId,
                            rangeStart,
                            rangeEnd
                    )
                    : timeSlotRepository.findBySpecialistIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                            specialistId,
                            status,
                            rangeStart,
                            rangeEnd
                    );
        } else {
            slots = status == null
                    ? timeSlotRepository.findBySpecialistIdOrderByStartTimeAsc(specialistId)
                    : timeSlotRepository.findBySpecialistIdAndStatusOrderByStartTimeAsc(specialistId, status);
        }
        return slots.stream().map(this::mapSlot).toList();
    }

    public TimeSlot getEntity(Long slotId) {
        return timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Time slot not found"));
    }

    private void validateSlotTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only future slots can be created");
        }
    }

    private SlotDtos.SlotResponse mapSlot(TimeSlot slot) {
        return new SlotDtos.SlotResponse(
                slot.getId(),
                slot.getSpecialist().getId(),
                slot.getSpecialist().getUser().getFullName(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
