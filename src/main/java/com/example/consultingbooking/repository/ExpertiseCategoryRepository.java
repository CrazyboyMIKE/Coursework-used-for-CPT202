package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.ExpertiseCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertiseCategoryRepository extends JpaRepository<ExpertiseCategory, Long> {

    Optional<ExpertiseCategory> findByNameIgnoreCase(String name);
}
