package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.SpecialistProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {

    Optional<SpecialistProfile> findByUserId(Long userId);

    List<SpecialistProfile> findByCategoryId(Long categoryId);

    @Query("""
            select p from SpecialistProfile p
            join p.user u
            join p.category c
            where :keyword is null
               or lower(u.fullName) like concat('%', :keyword, '%')
               or lower(u.username) like concat('%', :keyword, '%')
               or lower(c.name) like concat('%', :keyword, '%')
               or lower(p.level) like concat('%', :keyword, '%')
            """)
    Page<SpecialistProfile> searchForManagement(@Param("keyword") String keyword, Pageable pageable);
}
