package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.SessionToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {

    Optional<SessionToken> findByTokenAndActiveTrue(String token);

    List<SessionToken> findByUserIdAndActiveTrue(Long userId);

    void deleteByUserId(Long userId);
}
