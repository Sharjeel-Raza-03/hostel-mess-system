package com.hostel.mess.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_mess_count")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyMessCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_date", nullable = false, unique = true)
    private LocalDate countDate;

    @Builder.Default
    @Column(name = "breakfast_count")
    private Integer breakfastCount = 0;

    @Builder.Default
    @Column(name = "lunch_count")
    private Integer lunchCount = 0;

    @Builder.Default
    @Column(name = "dinner_count")
    private Integer dinnerCount = 0;
}