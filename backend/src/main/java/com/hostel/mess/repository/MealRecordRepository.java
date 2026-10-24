package com.hostel.mess.repository;

import com.hostel.mess.model.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    Optional<MealRecord> findByUserIdAndMealDate(Long userId, LocalDate mealDate);

    List<MealRecord> findByUserIdAndMealDateBetweenOrderByMealDate(Long userId, LocalDate from, LocalDate to);

    List<MealRecord> findByMealDate(LocalDate mealDate);
}