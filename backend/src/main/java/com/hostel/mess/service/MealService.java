package com.hostel.mess.service;

import com.hostel.mess.dto.MealRequest;
import com.hostel.mess.dto.MealResponse;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.exception.ResourceNotFoundException;
import com.hostel.mess.model.MealRecord;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.MealRecordRepository;
import com.hostel.mess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRecordRepository mealRepository;
    private final UserRepository userRepository;
    private final MessCountService messCountService;

    @Transactional
    public MealResponse saveMeal(String email, MealRequest request) {
        User user = findUser(email);

        MealRecord record = mealRepository
                .findByUserIdAndMealDate(user.getId(), request.mealDate())
                .orElse(null);

        if (record != null && record.isOnLeave()) {
            throw new BadRequestException("You are on approved leave on this date; meals cannot be changed");
        }
        if (record == null) {
            record = MealRecord.builder().user(user).mealDate(request.mealDate()).build();
        }

        record.setBreakfast(request.breakfast());
        record.setLunch(request.lunch());
        record.setDinner(request.dinner());
        record = mealRepository.save(record);

        messCountService.recalculate(request.mealDate());
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<MealResponse> getMyMeals(String email, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int m = month != null ? month : today.getMonthValue();
        int y = year != null ? year : today.getYear();
        if (m < 1 || m > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (y < 2000 || y > 2100) {
            throw new BadRequestException("Year is out of range");
        }

        LocalDate from = LocalDate.of(y, m, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        User user = findUser(email);

        return mealRepository
                .findByUserIdAndMealDateBetweenOrderByMealDate(user.getId(), from, to)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MealResponse> getMealsByDate(LocalDate date) {
        return mealRepository.findByMealDate(date).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(MealResponse::studentName))
                .toList();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private MealResponse toResponse(MealRecord r) {
        User u = r.getUser();
        return new MealResponse(r.getId(), u.getId(), u.getFullName(), u.getRoomNo(),
                r.getMealDate(), r.isBreakfast(), r.isLunch(), r.isDinner(), r.isOnLeave());
    }
}