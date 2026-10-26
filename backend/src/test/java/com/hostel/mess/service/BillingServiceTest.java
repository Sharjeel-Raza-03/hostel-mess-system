package com.hostel.mess.service;

import com.hostel.mess.dto.BillResponse;
import com.hostel.mess.dto.RatesResponse;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.model.Bill;
import com.hostel.mess.model.MealRecord;
import com.hostel.mess.model.Role;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.BillRepository;
import com.hostel.mess.repository.MealRecordRepository;
import com.hostel.mess.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    private static final BigDecimal BREAKFAST = new BigDecimal("80.00");
    private static final BigDecimal LUNCH = new BigDecimal("150.00");
    private static final BigDecimal DINNER = new BigDecimal("150.00");

    @Mock private MealRecordRepository mealRepository;
    @Mock private BillRepository billRepository;
    @Mock private UserRepository userRepository;
    @Mock private SettingsService settingsService;
    @InjectMocks private BillingService billingService;

    private static User student(long id, String name) {
        return User.builder().id(id).fullName(name).email("s" + id + "@test.com")
                .role(Role.STUDENT).roomNo("A-" + id).build();
    }

    private static MealRecord meal(User user, String date, boolean b, boolean l, boolean d, boolean onLeave) {
        return MealRecord.builder().user(user).mealDate(LocalDate.parse(date))
                .breakfast(b).lunch(l).dinner(d).onLeave(onLeave).build();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected " + expected + " but was " + actual);
    }

    @Test
    void calculateAmount_usesTheRateOfEachMealType() {
        BigDecimal amount = BillingService.calculateAmount(3, 3, 3, BREAKFAST, LUNCH, DINNER);
        assertMoney("1140.00", amount);
    }

    @Test
    void calculateAmount_withMixedMeals() {
        BigDecimal amount = BillingService.calculateAmount(2, 1, 0, BREAKFAST, LUNCH, DINNER);
        assertMoney("310.00", amount);
    }

    @Test
    void calculateAmount_withNoMeals_isZero() {
        BigDecimal amount = BillingService.calculateAmount(0, 0, 0, BREAKFAST, LUNCH, DINNER);
        assertMoney("0.00", amount);
    }

    @Test
    void calculateAndSave_ignoresDaysOnLeave() {
        User u = student(5L, "Ali Khan");
        List<MealRecord> records = List.of(
                meal(u, "2026-11-01", true, true, true, false),
                meal(u, "2026-11-02", true, true, true, false),
                meal(u, "2026-11-03", true, true, true, true));

        when(settingsService.getRates()).thenReturn(new RatesResponse(BREAKFAST, LUNCH, DINNER, 10));
        when(mealRepository.findByUserIdAndMealDateBetweenOrderByMealDate(
                5L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(records);
        when(billRepository.findByUserIdAndBillMonthAndBillYear(5L, 11, 2026)).thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        Bill bill = billingService.calculateAndSave(u, 11, 2026);

        assertEquals(6, bill.getTotalMeals().intValue());
        assertEquals(1, bill.getLeaveDays().intValue());
        assertMoney("760.00", bill.getTotalAmount());
        assertMoney("126.67", bill.getMealRate());
        assertEquals(11, bill.getBillMonth().intValue());
        assertEquals(2026, bill.getBillYear().intValue());
    }

    @Test
    void calculateAndSave_updatesTheExistingBillInsteadOfCreatingAnother() {
        User u = student(5L, "Ali Khan");
        Bill existing = Bill.builder().id(99L).user(u).billMonth(11).billYear(2026)
                .totalMeals(9).totalAmount(new BigDecimal("1140.00")).build();

        when(settingsService.getRates()).thenReturn(new RatesResponse(BREAKFAST, LUNCH, DINNER, 10));
        when(mealRepository.findByUserIdAndMealDateBetweenOrderByMealDate(
                eq(5L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(meal(u, "2026-11-01", true, false, false, false)));
        when(billRepository.findByUserIdAndBillMonthAndBillYear(5L, 11, 2026)).thenReturn(Optional.of(existing));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        Bill result = billingService.calculateAndSave(u, 11, 2026);

        assertSame(existing, result);
        assertEquals(99L, result.getId().longValue());
        assertEquals(1, result.getTotalMeals().intValue());
        assertMoney("80.00", result.getTotalAmount());
        verify(billRepository, times(1)).save(existing);
    }

    @Test
    void generateForAll_createsABillForEveryStudent() {
        User a = student(1L, "Ali Khan");
        User b = student(2L, "Sara Ahmed");

        when(userRepository.findByRoleOrderByFullName(Role.STUDENT)).thenReturn(List.of(a, b));
        when(settingsService.getRates()).thenReturn(new RatesResponse(BREAKFAST, LUNCH, DINNER, 10));
        when(mealRepository.findByUserIdAndMealDateBetweenOrderByMealDate(
                anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(billRepository.findByUserIdAndBillMonthAndBillYear(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        List<BillResponse> result = billingService.generateForAll(11, 2026);

        assertEquals(2, result.size());
        assertEquals("Ali Khan", result.get(0).studentName());
        verify(billRepository, times(2)).save(any(Bill.class));
    }

    @Test
    void generateForAll_withInvalidMonth_isRejected() {
        assertThrows(BadRequestException.class, () -> billingService.generateForAll(13, 2026));
        verifyNoInteractions(userRepository);
    }
}