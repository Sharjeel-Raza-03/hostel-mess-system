package com.hostel.mess.controller;

import com.hostel.mess.dto.BillResponse;
import com.hostel.mess.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillingService billingService;

    @GetMapping("/my")
    public ResponseEntity<List<BillResponse>> myBills(Authentication authentication) {
        return ResponseEntity.ok(billingService.getMyBills(authentication.getName()));
    }
}