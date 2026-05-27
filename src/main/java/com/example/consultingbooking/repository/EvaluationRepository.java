package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.Evaluation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
