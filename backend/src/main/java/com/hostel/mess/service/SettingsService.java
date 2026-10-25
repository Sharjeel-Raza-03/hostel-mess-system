package com.hostel.mess.service;

import com.hostel.mess.dto.RatesRequest;
import com.hostel.mess.dto.RatesResponse;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.model.Setting;
import com.hostel.mess.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    public static final String KEY_BREAKFAST = "MEAL_RATE_BREAKFAST";
    public static final String KEY_LUNCH = "MEAL_RATE_LUNCH";
    public static final String KEY_DINNER = "MEAL_RATE_DINNER";
    public static final String KEY_THRESHOLD = "LOW_STOCK_THRESHOLD";
    private static final int DEFAULT_THRESHOLD = 10;

    private final SettingRepository settingRepository;

    @Transactional(readOnly = true)
    public RatesResponse getRates() {
        Map<String, String> values = new HashMap<>();
        for (Setting s : settingRepository.findBySettingKeyIn(
                List.of(KEY_BREAKFAST, KEY_LUNCH, KEY_DINNER, KEY_THRESHOLD))) {
            if (s.getSettingValue() != null) {
                values.put(s.getSettingKey(), s.getSettingValue());
            }
        }
        return new RatesResponse(
                rate(values, KEY_BREAKFAST),
                rate(values, KEY_LUNCH),
                rate(values, KEY_DINNER),
                threshold(values));
    }

    @Transactional
    public RatesResponse update(RatesRequest request) {
        if (request.breakfastRate() == null && request.lunchRate() == null
                && request.dinnerRate() == null && request.lowStockThreshold() == null) {
            throw new BadRequestException("Provide at least one value to update");
        }
        if (request.breakfastRate() != null) save(KEY_BREAKFAST, money(request.breakfastRate()));
        if (request.lunchRate() != null) save(KEY_LUNCH, money(request.lunchRate()));
        if (request.dinnerRate() != null) save(KEY_DINNER, money(request.dinnerRate()));
        if (request.lowStockThreshold() != null) {
            save(KEY_THRESHOLD, String.valueOf(request.lowStockThreshold()));
        }
        return getRates();
    }

    private void save(String key, String value) {
        Setting setting = settingRepository.findById(key)
                .orElseGet(() -> Setting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal rate(Map<String, String> values, String key) {
        String raw = values.get(key);
        if (raw == null) {
            throw new IllegalStateException("Meal rate is not configured: " + key);
        }
        try {
            return new BigDecimal(raw.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Meal rate is not a number: " + key);
        }
    }

    private int threshold(Map<String, String> values) {
        String raw = values.get(KEY_THRESHOLD);
        if (raw == null) {
            return DEFAULT_THRESHOLD;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_THRESHOLD;
        }
    }
}