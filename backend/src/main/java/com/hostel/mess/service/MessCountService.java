package com.hostel.mess.service;

import com.hostel.mess.dto.MessCountResponse;
import com.hostel.mess.model.DailyMessCount;
import com.hostel.mess.model.MealRecord;
import com.hostel.mess.repository.DailyMessCountRepository;
import com.hostel.mess.repository.MealRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessCountService {

    private final MealRecordRepository mealRepository;
    private final DailyMessCountRepository countRepository;

    /** Recounts the meals for a date (ignoring students on leave) and saves the result. */
    @Transactional
    public DailyMessCount recalculate(LocalDate date) {
        List<MealRecord> records = mealRepository.findByMealDate(date);
        int breakfast = 0;
        int lunch = 0;
        int dinner = 0;
        for (MealRecord r : records) {
            if (r.isOnLeave()) {
                continue;
            }
            if (r.isBreakfast()) breakfast++;
            if (r.isLunch()) lunch++;
            if (r.isDinner()) dinner++;
        }

        DailyMessCount count = countRepository.findByCountDate(date)
                .orElseGet(() -> DailyMessCount.builder().countDate(date).build());
        count.setBreakfastCount(breakfast);
        count.setLunchCount(lunch);
        count.setDinnerCount(dinner);
        return countRepository.save(count);
    }

    @Transactional
    public MessCountResponse getCount(LocalDate date) {
        DailyMessCount c = recalculate(date);
        return new MessCountResponse(c.getCountDate(), c.getBreakfastCount(),
                c.getLunchCount(), c.getDinnerCount());
    }
}