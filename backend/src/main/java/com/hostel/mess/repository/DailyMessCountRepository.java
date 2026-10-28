package com.hostel.mess.repository;

import com.hostel.mess.model.DailyMessCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyMessCountRepository extends JpaRepository<DailyMessCount, Long> {

    Optional<DailyMessCount> findByCountDate(LocalDate countDate);

    /** Creates the row for a date if it does not exist yet. Safe when many requests do it at the same time. */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO daily_mess_count (count_date, breakfast_count, lunch_count, dinner_count) "
            + "VALUES (:date, 0, 0, 0) ON CONFLICT (count_date) DO NOTHING", nativeQuery = true)
    void ensureRow(@Param("date") LocalDate date);

    /** Locks the row of that date until the transaction ends, so concurrent recounts run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from DailyMessCount c where c.countDate = :date")
    Optional<DailyMessCount> findForUpdate(@Param("date") LocalDate date);
}