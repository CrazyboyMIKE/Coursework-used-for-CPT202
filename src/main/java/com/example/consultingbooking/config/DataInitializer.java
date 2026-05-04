package com.example.consultingbooking.config;

import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class DataInitializer {

    private static final String ADMIN_USERNAME = "AdminGroup28";
    private static final String LEGACY_ADMIN_USERNAME = "admin001";
    private static final String ADMIN_PASSWORD = "Group28_CPT202";
    private static final String ADMIN_EMAIL = "admingroup28@consultbridge.local";
    private static final String ADMIN_PHONE = "1000000000";
    private static final String SPECIALIST_PASSWORD = "Specialist123!";
    private static final List<SpecialistSeed> SPECIALIST_SEEDS = List.of(
            new SpecialistSeed("specialist01", "Ava Morgan", "Career Coaching", "ICF Certified Career Coach", "USD", new BigDecimal("120.00"), "Supports career transitions and interview preparation."),
            new SpecialistSeed("specialist02", "Ethan Brooks", "Financial Advisory", "Certified Financial Planner", "USD", new BigDecimal("150.00"), "Focuses on personal budgeting and financial planning."),
            new SpecialistSeed("specialist03", "Sophia Bennett", "Business Strategy", "Senior Strategy Consultant", "USD", new BigDecimal("180.00"), "Advises startups on business model design and growth strategy."),
            new SpecialistSeed("specialist04", "Liam Carter", "Academic Mentoring", "University Admissions Consultant", "USD", new BigDecimal("100.00"), "Guides students on study planning and application strategy."),
            new SpecialistSeed("specialist05", "Olivia Foster", "Immigration Consulting", "Registered Immigration Adviser", "USD", new BigDecimal("210.00"), "Provides consultation on documentation preparation and visa process planning."),
            new SpecialistSeed("specialist06", "Noah Simmons", "Leadership Development", "Executive Leadership Coach", "USD", new BigDecimal("230.00"), "Works with professionals on leadership growth and team communication."),
            new SpecialistSeed("specialist07", "Mia Reynolds", "Career Coaching", "LinkedIn Branding Specialist", "USD", new BigDecimal("115.00"), "Helps clients build stronger profiles and job-search positioning."),
            new SpecialistSeed("specialist08", "James Peterson", "Financial Advisory", "Investment Planning Adviser", "USD", new BigDecimal("175.00"), "Supports long-term personal investment planning for early professionals."),
            new SpecialistSeed("specialist09", "Charlotte Hughes", "Business Strategy", "Operations Improvement Consultant", "USD", new BigDecimal("165.00"), "Specializes in process review and operational efficiency planning."),
            new SpecialistSeed("specialist10", "Benjamin Ward", "Academic Mentoring", "Graduate Application Mentor", "USD", new BigDecimal("95.00"), "Supports graduate-school applications and personal statement refinement."),
            new SpecialistSeed("specialist11", "Amelia Hayes", "Immigration Consulting", "Cross-border Mobility Consultant", "USD", new BigDecimal("220.00"), "Advises on mobility planning for skilled migration cases."),
            new SpecialistSeed("specialist12", "Lucas Price", "Leadership Development", "People Management Trainer", "USD", new BigDecimal("205.00"), "Supports new managers with delegation, feedback, and team planning."),
            new SpecialistSeed("specialist13", "Harper Cole", "Career Coaching", "Career Pivot Consultant", "USD", new BigDecimal("130.00"), "Works with mid-career professionals seeking industry transitions."),
            new SpecialistSeed("specialist14", "Henry Russell", "Financial Advisory", "Retirement Planning Specialist", "USD", new BigDecimal("190.00"), "Focuses on retirement planning and long-term savings structure."),
            new SpecialistSeed("specialist15", "Ella Powell", "Business Strategy", "Market Entry Consultant", "USD", new BigDecimal("240.00"), "Helps teams evaluate expansion and market-entry strategy."),
            new SpecialistSeed("specialist16", "Alexander Perry", "Academic Mentoring", "Scholarship Application Coach", "USD", new BigDecimal("110.00"), "Supports scholarship strategy and interview readiness."),
            new SpecialistSeed("specialist17", "Grace Long", "Immigration Consulting", "Documentation Review Specialist", "USD", new BigDecimal("160.00"), "Reviews application evidence and case preparation strategy."),
            new SpecialistSeed("specialist18", "Daniel Ross", "Leadership Development", "Workplace Communication Coach", "USD", new BigDecimal("145.00"), "Helps clients strengthen communication and conflict-management skills."),
            new SpecialistSeed("specialist19", "Chloe Murphy", "Career Coaching", "Resume and Interview Consultant", "USD", new BigDecimal("125.00"), "Provides practical coaching on resume design and mock interviews."),
            new SpecialistSeed("specialist20", "Michael Cooper", "Business Strategy", "Product Growth Adviser", "USD", new BigDecimal("215.00"), "Supports product teams with positioning, growth, and roadmap thinking.")
    );

    @Bean
    CommandLineRunner seedDemoData(
            UserAccountRepository userAccountRepository,
            ExpertiseCategoryRepository expertiseCategoryRepository,
            SpecialistProfileRepository specialistProfileRepository,
            TimeSlotRepository timeSlotRepository
    ) {
        return args -> {
            ensureAdminAccount(userAccountRepository);
            Map<String, ExpertiseCategory> categoryMap = new HashMap<>();
            categoryMap.put("Career Coaching", ensureCategory(expertiseCategoryRepository, "Career Coaching", "Career planning and interview preparation"));
            categoryMap.put("Financial Advisory", ensureCategory(expertiseCategoryRepository, "Financial Advisory", "Personal financial planning"));
            categoryMap.put("Business Strategy", ensureCategory(expertiseCategoryRepository, "Business Strategy", "Business model planning and operational strategy"));
            categoryMap.put("Academic Mentoring", ensureCategory(expertiseCategoryRepository, "Academic Mentoring", "Study planning, admissions, and academic guidance"));
            categoryMap.put("Immigration Consulting", ensureCategory(expertiseCategoryRepository, "Immigration Consulting", "Migration planning and document preparation support"));
            categoryMap.put("Leadership Development", ensureCategory(expertiseCategoryRepository, "Leadership Development", "Leadership coaching and management capability building"));
            ensureSpecialistFixtures(userAccountRepository, specialistProfileRepository, timeSlotRepository, categoryMap);
        };
    }

    private void ensureAdminAccount(UserAccountRepository userAccountRepository) {
        UserAccount admin = userAccountRepository.findByUsernameIgnoreCase(ADMIN_USERNAME)
                .or(() -> userAccountRepository.findByUsernameIgnoreCase(LEGACY_ADMIN_USERNAME))
                .orElseGet(UserAccount::new);

        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(ADMIN_PASSWORD);
        admin.setFullName("Platform Administrator");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPhone(ADMIN_PHONE);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userAccountRepository.save(admin);
    }

    private ExpertiseCategory ensureCategory(
            ExpertiseCategoryRepository expertiseCategoryRepository,
            String name,
            String description
    ) {
        ExpertiseCategory category = expertiseCategoryRepository.findByNameIgnoreCase(name)
                .orElseGet(ExpertiseCategory::new);
        category.setName(name);
        category.setDescription(description);
        category.setActive(true);
        return expertiseCategoryRepository.save(category);
    }

    private void ensureSpecialistFixtures(
            UserAccountRepository userAccountRepository,
            SpecialistProfileRepository specialistProfileRepository,
            TimeSlotRepository timeSlotRepository,
            Map<String, ExpertiseCategory> categoryMap
    ) {
        for (int index = 0; index < SPECIALIST_SEEDS.size(); index++) {
            SpecialistSeed seed = SPECIALIST_SEEDS.get(index);
            SpecialistProfile profile = ensureSpecialistProfile(userAccountRepository, specialistProfileRepository, categoryMap, seed, index);
            ensureTimeSlots(timeSlotRepository, profile, index);
        }
    }

    private SpecialistProfile ensureSpecialistProfile(
            UserAccountRepository userAccountRepository,
            SpecialistProfileRepository specialistProfileRepository,
            Map<String, ExpertiseCategory> categoryMap,
            SpecialistSeed seed,
            int index
    ) {
        UserAccount specialistUser = userAccountRepository.findByUsernameIgnoreCase(seed.username())
                .orElseGet(UserAccount::new);
        specialistUser.setUsername(seed.username());
        specialistUser.setPassword(SPECIALIST_PASSWORD);
        specialistUser.setFullName(seed.fullName());
        specialistUser.setEmail(seed.username() + "@consultbridge.local");
        specialistUser.setPhone(String.format("1551000%04d", index + 1));
        specialistUser.setRole(UserRole.SPECIALIST);
        specialistUser.setActive(true);
        specialistUser = userAccountRepository.save(specialistUser);

        SpecialistProfile profile = specialistProfileRepository.findByUserId(specialistUser.getId())
                .orElseGet(SpecialistProfile::new);
        profile.setUser(specialistUser);
        profile.setCategory(categoryMap.get(seed.categoryName()));
        profile.setLevel(seed.level());
        profile.setBaseFee(seed.baseFee());
        profile.setFeeCurrency("USD");
        profile.setStatus(SpecialistStatus.ACTIVE);
        profile.setBio(seed.bio());
        return specialistProfileRepository.save(profile);
    }

    private void ensureTimeSlots(
            TimeSlotRepository timeSlotRepository,
            SpecialistProfile profile,
            int index
    ) {
        int[] hours = {9, 11, 14};

        for (int slotIndex = 0; slotIndex < hours.length; slotIndex++) {
            LocalDateTime startTime = nextBusinessSlot(index + slotIndex + 1, hours[slotIndex] + (index % 2));
            LocalDateTime endTime = startTime.plusHours(1);

            if (timeSlotRepository.existsBySpecialistIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    profile.getId(),
                    endTime,
                    startTime
            )) {
                continue;
            }

            TimeSlot slot = new TimeSlot();
            slot.setSpecialist(profile);
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);
            slot.setStatus(SlotStatus.AVAILABLE);
            timeSlotRepository.save(slot);
        }
    }

    private LocalDateTime nextBusinessSlot(int daysAhead, int hour) {
        LocalDateTime slotTime = LocalDateTime.now()
                .plusDays(daysAhead)
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        while (slotTime.getDayOfWeek() == DayOfWeek.SATURDAY || slotTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            slotTime = slotTime.plusDays(1);
        }

        if (!slotTime.isAfter(LocalDateTime.now())) {
            slotTime = slotTime.plusDays(1);
        }

        return slotTime;
    }

    private record SpecialistSeed(
            String username,
            String fullName,
            String categoryName,
            String level,
            String feeCurrency,
            BigDecimal baseFee,
            String bio
    ) {
    }

}
