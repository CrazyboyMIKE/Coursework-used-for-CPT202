package com.example.consultingbooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "specialist_profiles")
public class SpecialistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpertiseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpecialistLevel level;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpecialistStatus status;

    @Column(length = 500)
    private String bio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public ExpertiseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpertiseCategory category) {
        this.category = category;
    }

    public SpecialistLevel getLevel() {
        return level;
    }

    public void setLevel(SpecialistLevel level) {
        this.level = level;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public SpecialistStatus getStatus() {
        return status;
    }

    public void setStatus(SpecialistStatus status) {
        this.status = status;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
