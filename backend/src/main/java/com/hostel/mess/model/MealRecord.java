package com.hostel.mess.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "meal_records")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MealRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Builder.Default
    private boolean breakfast = false;

    @Builder.Default
    private boolean lunch = false;

    @Builder.Default
    private boolean dinner = false;

    @Builder.Default
    @Column(name = "on_leave")
    private boolean onLeave = false;
}