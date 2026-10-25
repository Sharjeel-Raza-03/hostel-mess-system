package com.hostel.mess.controller;

import com.hostel.mess.dto.MedicineResponse;
import com.hostel.mess.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public ResponseEntity<List<MedicineResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        return ResponseEntity.ok(medicineService.search(search, category, availableOnly));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(medicineService.getCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getById(id));
    }
}