package com.example.consultingbooking;

import com.example.consultingbooking.dto.SlotDtos;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.service.SlotService;
import com.example.consultingbooking.service.SpecialistService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SlotServiceTest {

    private TimeSlotRepository timeSlotRepository;
    private SpecialistService specialistService;
    private SlotService slotService;
    private UserAccount specialistUser;
    private SpecialistProfile specialist;

    @BeforeEach
    void setUp() {
        timeSlotRepository = Mockito.mock(TimeSlotRepository.class);
        specialistService = Mockito.mock(SpecialistService.class);
        slotService = new SlotService(timeSlotRepository, specialistService);

        specialistUser = new UserAccount();
        specialistUser.setId(20L);
        specialistUser.setRole(UserRole.SPECIALIST);

        ExpertiseCategory category = new ExpertiseCategory();
        category.setId(7L);
        category.setName("Strategy");

        specialist = new SpecialistProfile();
        specialist.setId(4L);
        specialist.setUser(specialistUser);
        specialist.setCategory(category);
        specialist.setLevel("Senior Consultant");
    }

    @Test
    void createSlotRejectsEndTimeBeforeStartTime() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);

        Assertions.assertThrows(BusinessException.class, () -> slotService.createSlot(
                specialistUser,
                4L,
                new SlotDtos.SlotRequest(start, start.minusHours(1))
        ));

        Mockito.verifyNoInteractions(specialistService);
        Mockito.verifyNoInteractions(timeSlotRepository);
    }

    @Test
    void createSlotRejectsOverlappingSlot() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        Mockito.when(specialistService.getEntity(4L)).thenReturn(specialist);
        Mockito.when(timeSlotRepository.existsBySpecialistIdAndStartTimeLessThanAndEndTimeGreaterThan(4L, end, start))
                .thenReturn(true);

        Assertions.assertThrows(BusinessException.class, () -> slotService.createSlot(
                specialistUser,
                4L,
                new SlotDtos.SlotRequest(start, end)
        ));

        Mockito.verify(specialistService).ensureOwnerOrAdmin(specialistUser, specialist);
        Mockito.verify(timeSlotRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void createSlotSavesAvailableSlotWhenTimeIsValidAndFree() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        Mockito.when(specialistService.getEntity(4L)).thenReturn(specialist);
        Mockito.when(timeSlotRepository.existsBySpecialistIdAndStartTimeLessThanAndEndTimeGreaterThan(4L, end, start))
                .thenReturn(false);
        Mockito.when(timeSlotRepository.save(Mockito.any(TimeSlot.class))).thenAnswer(invocation -> {
            TimeSlot slot = invocation.getArgument(0);
            slot.setId(99L);
            return slot;
        });

        SlotDtos.SlotResponse response = slotService.createSlot(
                specialistUser,
                4L,
                new SlotDtos.SlotRequest(start, end)
        );

        ArgumentCaptor<TimeSlot> captor = ArgumentCaptor.forClass(TimeSlot.class);
        Mockito.verify(timeSlotRepository).save(captor.capture());
        Assertions.assertEquals(99L, response.id());
        Assertions.assertEquals(4L, response.specialistId());
        Assertions.assertEquals(start, captor.getValue().getStartTime());
        Assertions.assertEquals(end, captor.getValue().getEndTime());
    }
}
