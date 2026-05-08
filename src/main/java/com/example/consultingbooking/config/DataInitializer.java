package com.example.consultingbooking.config;

import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.SlotStatus;
import com.example.consultingbooking.entity.SpecialistProfile;
import com.example.consultingbooking.entity.SpecialistStatus;
import com.example.consultingbooking.entity.TimeSlot;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.repository.BookingRepository;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.PasswordResetTokenRepository;
import com.example.consultingbooking.repository.SessionTokenRepository;
import com.example.consultingbooking.repository.SpecialistProfileRepository;
import com.example.consultingbooking.repository.TimeSlotRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!test")
public class DataInitializer {

    private static final String ADMIN_USERNAME = "AdminGroup28";
    private static final String LEGACY_ADMIN_USERNAME = "admin001";
    private static final String ADMIN_PASSWORD = "Group28_CPT202";
    private static final String ADMIN_EMAIL = "admingroup28@consultbridge.local";
    private static final String ADMIN_PHONE = "1000000000";
    private static final String SPECIALIST_PASSWORD = "Specialist123!";
    private static final List<CustomerSeed> CUSTOMER_SEEDS = List.of(
            new CustomerSeed("customer001", "Customer 001", "12345671001@gmail.com", "123456789001", "12345678001"),
            new CustomerSeed("customer002", "Customer 002", "12345671002@gmail.com", "123456789002", "12345678002"),
            new CustomerSeed("customer003", "Customer 003", "12345671003@gmail.com", "123456789003", "12345678003"),
            new CustomerSeed("customer004", "Customer 004", "12345671004@gmail.com", "123456789004", "12345678004"),
            new CustomerSeed("customer005", "Customer 005", "12345671005@gmail.com", "123456789005", "12345678005"),
            new CustomerSeed("customer006", "Customer 006", "12345671006@gmail.com", "123456789006", "12345678006"),
            new CustomerSeed("customer007", "Customer 007", "12345671007@gmail.com", "123456789007", "12345678007"),
            new CustomerSeed("customer008", "Customer 008", "12345671008@gmail.com", "123456789008", "12345678008"),
            new CustomerSeed("customer009", "Customer 009", "12345671009@gmail.com", "123456789009", "12345678009"),
            new CustomerSeed("customer010", "Customer 010", "12345671010@gmail.com", "123456789010", "12345678010")
    );
    private static final List<SpecialistSeed> SPECIALIST_SEEDS = buildSpecialistSeeds();

    @Bean
    CommandLineRunner seedDemoData(
            UserAccountRepository userAccountRepository,
            ExpertiseCategoryRepository expertiseCategoryRepository,
            BookingRepository bookingRepository,
            SessionTokenRepository sessionTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            SpecialistProfileRepository specialistProfileRepository,
            TimeSlotRepository timeSlotRepository,
            PlatformTransactionManager transactionManager
    ) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return args -> {
            transactionTemplate.executeWithoutResult(status -> {
                ensureAdminAccount(userAccountRepository);
                ensureCustomerFixtures(userAccountRepository);
                Map<String, ExpertiseCategory> categoryMap = new HashMap<>();
                categoryMap.put("Financial Advisory", ensureCategory(expertiseCategoryRepository, "Financial Advisory", "Personal financial planning"));
                categoryMap.put("Investment Advisory", ensureCategory(expertiseCategoryRepository, "Investment Advisory", "Investment planning, portfolio structure, and asset-allocation support"));
                categoryMap.put("Legal Consulting", ensureCategory(expertiseCategoryRepository, "Legal Consulting", "Legal consultation, document review, and compliance guidance"));
                purgeLegacySpecialistFixtures(
                        userAccountRepository,
                        bookingRepository,
                        sessionTokenRepository,
                        passwordResetTokenRepository,
                        specialistProfileRepository,
                        timeSlotRepository
                );
                ensureSpecialistFixtures(userAccountRepository, specialistProfileRepository, timeSlotRepository, categoryMap);
            });
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

    private void ensureCustomerFixtures(UserAccountRepository userAccountRepository) {
        for (CustomerSeed seed : CUSTOMER_SEEDS) {
            UserAccount customer = userAccountRepository.findByUsernameIgnoreCase(seed.username())
                    .orElseGet(UserAccount::new);
            customer.setUsername(seed.username());
            customer.setPassword(seed.password());
            customer.setFullName(seed.fullName());
            customer.setEmail(seed.email());
            customer.setPhone(seed.phone());
            customer.setRole(UserRole.CUSTOMER);
            customer.setActive(true);
            userAccountRepository.save(customer);
        }
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

    private void purgeLegacySpecialistFixtures(
            UserAccountRepository userAccountRepository,
            BookingRepository bookingRepository,
            SessionTokenRepository sessionTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            SpecialistProfileRepository specialistProfileRepository,
            TimeSlotRepository timeSlotRepository
    ) {
        for (String username : specialistFixtureUsernamesToPurge()) {
            userAccountRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
                bookingRepository.deleteAll(bookingRepository.findBySpecialistUserIdOrderBySlotStartTimeAsc(user.getId()));
                sessionTokenRepository.deleteByUserId(user.getId());
                passwordResetTokenRepository.deleteByUserId(user.getId());

                specialistProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
                    List<TimeSlot> slots = timeSlotRepository.findBySpecialistIdOrderByStartTimeAsc(profile.getId());
                    if (!slots.isEmpty()) {
                        timeSlotRepository.deleteAll(slots);
                    }
                    specialistProfileRepository.delete(profile);
                });

                userAccountRepository.delete(user);
            });
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
        specialistUser.setEmail(seed.email());
        specialistUser.setPhone(seed.phone());
        specialistUser.setRole(UserRole.SPECIALIST);
        specialistUser.setActive(true);
        specialistUser = userAccountRepository.save(specialistUser);

        SpecialistProfile profile = specialistProfileRepository.findByUserId(specialistUser.getId())
                .orElseGet(SpecialistProfile::new);
        profile.setUser(specialistUser);
        profile.setCategory(categoryMap.get(seed.categoryName()));
        profile.setLevel(seed.level());
        profile.setBaseFee(seed.baseFee());
        profile.setFeeCurrency(seed.feeCurrency());
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

    private static List<SpecialistSeed> buildSpecialistSeeds() {
        List<SpecialistSeed> seeds = new ArrayList<>();
        String[] categories = {
                "Financial Advisory",
                "Investment Advisory",
                "Legal Consulting"
        };
        String[] credentials = {
                "CFP Certification",
                "CFA Charterholder",
                "JD",
                "Series 7 License",
                "Series 65 License",
                "LLM in Corporate Law",
                "ChFC Certification",
                "FRM Part II Candidate",
                "Bar Admission (New York)",
                "CIMA Certification",
                "CFA Level III Candidate",
                "Bar Admission (California)",
                "Retirement Income Certified Professional",
                "CMT Level II",
                "Solicitor Qualification (England and Wales)",
                "Certified Private Wealth Advisor",
                "CAIA Charterholder",
                "Certificate in Contract Law",
                "Series 66 License",
                "Investment Adviser Representative License",
                "LLM in Commercial Law",
                "Accredited Estate Planner",
                "Certificate in Securities Analysis",
                "Bar Admission (Texas)",
                "Certified Divorce Financial Analyst"
        };
        BigDecimal[] fees = {
                new BigDecimal("145.00"),
                new BigDecimal("185.00"),
                new BigDecimal("240.00"),
                new BigDecimal("155.00"),
                new BigDecimal("190.00"),
                new BigDecimal("255.00"),
                new BigDecimal("165.00"),
                new BigDecimal("205.00"),
                new BigDecimal("235.00"),
                new BigDecimal("175.00"),
                new BigDecimal("210.00"),
                new BigDecimal("245.00"),
                new BigDecimal("170.00"),
                new BigDecimal("215.00"),
                new BigDecimal("260.00"),
                new BigDecimal("195.00"),
                new BigDecimal("225.00"),
                new BigDecimal("250.00"),
                new BigDecimal("180.00"),
                new BigDecimal("220.00"),
                new BigDecimal("265.00"),
                new BigDecimal("200.00"),
                new BigDecimal("230.00"),
                new BigDecimal("270.00"),
                new BigDecimal("205.00")
        };
        String[] bios = {
                "Provides structured planning for savings, budgeting, and medium-term financial decisions.",
                "Supports retail clients with portfolio planning and long-term investment conversations.",
                "Offers legal consultation support for contract review, compliance preparation, and case planning."
        };

        for (int index = 0; index < 20; index++) {
            int specialistNumber = index + 1;
            String formattedNumber = String.format("%03d", specialistNumber);
            String category = categories[index % categories.length];
            String bio = bios[index % bios.length];

            seeds.add(new SpecialistSeed(
                    "specialist" + formattedNumber,
                    "ABC" + formattedNumber,
                    "specialist" + formattedNumber + "@exigenctcommunication.local",
                    String.format("1669000%04d", specialistNumber),
                    category,
                    credentials[index],
                    "USD",
                    fees[index],
                    bio
            ));
        }

        return List.copyOf(seeds);
    }

    private static Set<String> specialistFixtureUsernamesToPurge() {
        Set<String> usernames = new LinkedHashSet<>();

        for (int number = 1; number <= 30; number++) {
            usernames.add(String.format("specialist%02d", number));
            usernames.add(String.format("specialist%03d", number));
            usernames.add("specialist" + number);
        }

        return usernames;
    }

    private record SpecialistSeed(
            String username,
            String fullName,
            String email,
            String phone,
            String categoryName,
            String level,
            String feeCurrency,
            BigDecimal baseFee,
            String bio
    ) {
    }

    private record CustomerSeed(
            String username,
            String fullName,
            String email,
            String phone,
            String password
    ) {
    }

}
