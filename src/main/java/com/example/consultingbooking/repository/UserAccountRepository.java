package com.example.consultingbooking.repository;

import com.example.consultingbooking.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
            select u from UserAccount u
            where :keyword is null
               or lower(u.username) like concat('%', :keyword, '%')
               or lower(u.fullName) like concat('%', :keyword, '%')
               or lower(u.email) like concat('%', :keyword, '%')
               or lower(coalesce(u.phone, '')) like concat('%', :keyword, '%')
            """)
    Page<UserAccount> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
