package com.hostel.mess.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bill_month", nullable = false)
    private Integer billMonth;

    @Column(name = "bill_year", nullable = false)
    private Integer billYear;

    @Column(name = "total_meals")
    private Integer totalMeals;

    @Column(name = "leave_days")
    private Integer leaveDays;

    @Column(name = "meal_rate", precision = 10, scale = 2)
    private BigDecimal mealRate;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    void onCreate() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }
}