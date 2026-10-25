package com.hostel.mess.controller;

import com.hostel.mess.dto.LeaveResponse;
import com.hostel.mess.dto.MealResponse;
import com.hostel.mess.dto.MessCountResponse;
import com.hostel.mess.dto.ReviewRequest;
import com.hostel.mess.model.LeaveStatus;
import com.hostel.mess.service.LeaveService;
import com.hostel.mess.service.MealService;
import com.hostel.mess.service.MessCountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LeaveService leaveService;
    private final MealService mealService;
    private final MessCountService messCountService;

    @GetMapping("/leaves")
    public ResponseEntity<List<LeaveResponse>> leaves(@RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(leaveService.getAll(status));
    }

    @PutMapping("/leaves/{id}/approve")
    public ResponseEntity<LeaveResponse> approve(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) ReviewRequest review,
                                                 Authentication authentication) {
        return ResponseEntity.ok(leaveService.approve(id, authentication.getName(), review));
    }

    @PutMapping("/leaves/{id}/reject")
    public ResponseEntity<LeaveResponse> reject(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) ReviewRequest review,
                                                Authentication authentication) {
        return ResponseEntity.ok(leaveService.reject(id, authentication.getName(), review));
    }

    @GetMapping("/mess-count")
    public ResponseEntity<MessCountResponse> messCount(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(messCountService.getCount(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/meals")
    public ResponseEntity<List<MealResponse>> meals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(mealService.getMealsByDate(date != null ? date : LocalDate.now()));
    }
}