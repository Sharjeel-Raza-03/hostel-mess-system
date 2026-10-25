package com.hostel.mess.service;

import com.hostel.mess.dto.BillResponse;
import com.hostel.mess.dto.MonthlyReportResponse;
import com.hostel.mess.dto.RatesResponse;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.exception.ResourceNotFoundException;
import com.hostel.mess.model.Bill;
import com.hostel.mess.model.MealRecord;
import com.hostel.mess.model.Role;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.BillRepository;
import com.hostel.mess.repository.MealRecordRepository;
import com.hostel.mess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final MealRecordRepository mealRepository;
    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final SettingsService settingsService;

    /** Rule 9: (breakfasts x rate) + (lunches x rate) + (dinners x rate). */
    public static BigDecimal calculateAmount(int breakfasts, int lunches, int dinners,
                                             BigDecimal breakfastRate, BigDecimal lunchRate,
                                             BigDecimal dinnerRate) {
        return breakfastRate.multiply(BigDecimal.valueOf(breakfasts))
                .add(lunchRate.multiply(BigDecimal.valueOf(lunches)))
                .add(dinnerRate.multiply(BigDecimal.valueOf(dinners)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Rule 10: creates the bill, or updates it if one already exists for that student and month. */
    @Transactional
    public Bill calculateAndSave(User user, int month, int year) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        int breakfasts = 0;
        int lunches = 0;
        int dinners = 0;
        int leaveDays = 0;
        List<MealRecord> records = mealRepository
                .findByUserIdAndMealDateBetweenOrderByMealDate(user.getId(), from, to);
        for (MealRecord r : records) {
            if (r.isOnLeave()) {
                leaveDays++;
                continue;
            }
            if (r.isBreakfast()) breakfasts++;
            if (r.isLunch()) lunches++;
            if (r.isDinner()) dinners++;
        }

        RatesResponse rates = settingsService.getRates();
        BigDecimal amount = calculateAmount(breakfasts, lunches, dinners,
                rates.breakfastRate(), rates.lunchRate(), rates.dinnerRate());
        int totalMeals = breakfasts + lunches + dinners;
        BigDecimal averageRate = totalMeals > 0
                ? amount.divide(BigDecimal.valueOf(totalMeals), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        Bill bill = billRepository.findByUserIdAndBillMonthAndBillYear(user.getId(), month, year)
                .orElseGet(() -> Bill.builder().user(user).billMonth(month).billYear(year).build());
        bill.setTotalMeals(totalMeals);
        bill.setLeaveDays(leaveDays);
        bill.setMealRate(averageRate);
        bill.setTotalAmount(amount);
        bill.setGeneratedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    @Transactional
    public List<BillResponse> generateForAll(int month, int year) {
        validatePeriod(month, year);
        return userRepository.findByRoleOrderByFullName(Role.STUDENT).stream()
                .map(u -> toResponse(calculateAndSave(u, month, year)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getMyBills(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return billRepository.findByUserIdOrderByBillYearDescBillMonthDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsForMonth(int month, int year) {
        validatePeriod(month, year);
        return billRepository.findByBillMonthAndBillYear(month, year).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(BillResponse::studentName))
                .toList();
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse monthlyReport(int month, int year) {
        validatePeriod(month, year);
        List<Bill> bills = billRepository.findByBillMonthAndBillYear(month, year);

        int meals = 0;
        int leaveDays = 0;
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Bill b : bills) {
            meals += b.getTotalMeals() == null ? 0 : b.getTotalMeals();
            leaveDays += b.getLeaveDays() == null ? 0 : b.getLeaveDays();
            if (b.getTotalAmount() != null) {
                revenue = revenue.add(b.getTotalAmount());
            }
        }
        return new MonthlyReportResponse(month, year, bills.size(), meals, leaveDays, revenue);
    }

    private void validatePeriod(int month, int year) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > 2100) {
            throw new BadRequestException("Year is out of range");
        }
    }

    private BillResponse toResponse(Bill b) {
        User u = b.getUser();
        return new BillResponse(b.getId(), u.getId(), u.getFullName(), u.getRoomNo(),
                b.getBillMonth(), b.getBillYear(), b.getTotalMeals(), b.getLeaveDays(),
                b.getMealRate(), b.getTotalAmount(), b.getGeneratedAt());
    }
}