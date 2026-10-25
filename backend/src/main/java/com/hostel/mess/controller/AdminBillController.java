package com.hostel.mess.controller;

import com.hostel.mess.dto.BillResponse;
import com.hostel.mess.dto.MonthlyReportResponse;
import com.hostel.mess.dto.RatesRequest;
import com.hostel.mess.dto.RatesResponse;
import com.hostel.mess.service.BillingService;
import com.hostel.mess.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBillController {

    private final BillingService billingService;
    private final SettingsService settingsService;

    @PostMapping("/bills/generate")
    public ResponseEntity<List<BillResponse>> generate(@RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(billingService.generateForAll(month, year));
    }

    @GetMapping("/bills")
    public ResponseEntity<List<BillResponse>> bills(@RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(billingService.getBillsForMonth(month, year));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<MonthlyReportResponse> report(@RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(billingService.monthlyReport(month, year));
    }

    @GetMapping("/settings/rates")
    public ResponseEntity<RatesResponse> getRates() {
        return ResponseEntity.ok(settingsService.getRates());
    }

    @PutMapping("/settings/rates")
    public ResponseEntity<RatesResponse> updateRates(@Valid @RequestBody RatesRequest request) {
        return ResponseEntity.ok(settingsService.update(request));
    }
}