package com.example.consultingbooking.config;

import com.example.consultingbooking.entity.ExpertiseCategory;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.repository.ExpertiseCategoryRepository;
import com.example.consultingbooking.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class DataInitializer {

    private static final String ADMIN_USERNAME = "AdminGroup28";
    private static final String LEGACY_ADMIN_USERNAME = "admin001";
    private static final String ADMIN_PASSWORD = "Group28_CPT204";
    private static final String ADMIN_EMAIL = "admingroup28@consultbridge.local";
    private static final String ADMIN_PHONE = "1000000000";

    @Bean
    CommandLineRunner seedDemoData(
            UserAccountRepository userAccountRepository,
            ExpertiseCategoryRepository expertiseCategoryRepository
    ) {
        return args -> {
            ensureAdminAccount(userAccountRepository);
            ensureCategory(expertiseCategoryRepository, "Career Coaching", "Career planning and interview preparation");
            ensureCategory(expertiseCategoryRepository, "Financial Advisory", "Personal financial planning");
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

    private void ensureCategory(
            ExpertiseCategoryRepository expertiseCategoryRepository,
            String name,
            String description
    ) {
        ExpertiseCategory category = expertiseCategoryRepository.findByNameIgnoreCase(name)
                .orElseGet(ExpertiseCategory::new);
        category.setName(name);
        category.setDescription(description);
        category.setActive(true);
        expertiseCategoryRepository.save(category);
    }

}
