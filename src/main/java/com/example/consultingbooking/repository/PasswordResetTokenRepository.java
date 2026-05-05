package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndActiveTrue(String token);

    List<PasswordResetToken> findByUserIdAndActiveTrue(Long userId);

    void deleteByUserId(Long userId);
}
