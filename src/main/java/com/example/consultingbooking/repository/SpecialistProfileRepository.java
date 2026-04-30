package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.SpecialistProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {

    Optional<SpecialistProfile> findByUserId(Long userId);

    List<SpecialistProfile> findByCategoryId(Long categoryId);
}
