package com.hostel.mess.service;

import com.hostel.mess.dto.MealRequest;
import com.hostel.mess.dto.MealResponse;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.model.MealRecord;
import com.hostel.mess.model.Role;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.MealRecordRepository;
import com.hostel.mess.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealServiceTest {

    private static final String EMAIL = "student@test.com";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    @Mock private MealRecordRepository mealRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessCountService messCountService;
    @InjectMocks private MealService mealService;

    private static User student() {
        return User.builder().id(2L).fullName("Test Student").email(EMAIL)
                .role(Role.STUDENT).roomNo("A-101").build();
    }

    @Test
    void saveMeal_onADateMarkedOnLeave_isRejected() {
        User student = student();
        MealRecord onLeave = MealRecord.builder().user(student).mealDate(DAY).onLeave(true).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(mealRepository.findByUserIdAndMealDate(2L, DAY)).thenReturn(Optional.of(onLeave));

        assertThrows(BadRequestException.class,
                () -> mealService.saveMeal(EMAIL, new MealRequest(DAY, true, true, true)));
        verify(mealRepository, never()).save(any(MealRecord.class));
        verifyNoInteractions(messCountService);
    }

    @Test
    void saveMeal_newRecord_isSavedAndTheDailyCountIsRefreshed() {
        User student = student();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(mealRepository.findByUserIdAndMealDate(2L, DAY)).thenReturn(Optional.empty());
        when(mealRepository.save(any(MealRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MealResponse response = mealService.saveMeal(EMAIL, new MealRequest(DAY, true, false, true));

        assertTrue(response.breakfast());
        assertFalse(response.lunch());
        assertTrue(response.dinner());
        assertFalse(response.onLeave());
        verify(messCountService).recalculate(DAY);
    }

    @Test
    void getMyMeals_withInvalidMonth_isRejected() {
        assertThrows(BadRequestException.class, () -> mealService.getMyMeals(EMAIL, 13, 2026));
    }

    @Test
    void getMyMeals_withInvalidYear_isRejected() {
        assertThrows(BadRequestException.class, () -> mealService.getMyMeals(EMAIL, 10, 1900));
    }
}