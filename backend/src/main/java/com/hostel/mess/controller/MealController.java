package com.hostel.mess.controller;

import com.hostel.mess.dto.MealRequest;
import com.hostel.mess.dto.MealResponse;
import com.hostel.mess.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping
    public ResponseEntity<MealResponse> save(@Valid @RequestBody MealRequest request,
                                             Authentication authentication) {
        return ResponseEntity.ok(mealService.saveMeal(authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<MealResponse>> myMeals(@RequestParam(required = false) Integer month,
                                                      @RequestParam(required = false) Integer year,
                                                      Authentication authentication) {
        return ResponseEntity.ok(mealService.getMyMeals(authentication.getName(), month, year));
    }
}