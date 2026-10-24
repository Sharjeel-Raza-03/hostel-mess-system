package com.hostel.mess.repository;

import com.hostel.mess.model.DailyMessCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyMessCountRepository extends JpaRepository<DailyMessCount, Long> {

    Optional<DailyMessCount> findByCountDate(LocalDate countDate);
}