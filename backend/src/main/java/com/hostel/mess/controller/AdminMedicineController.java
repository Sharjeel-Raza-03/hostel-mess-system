package com.hostel.mess.controller;

import com.hostel.mess.dto.MedicineRequest;
import com.hostel.mess.dto.MedicineResponse;
import com.hostel.mess.dto.StockUpdateRequest;
import com.hostel.mess.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/medicines")
@RequiredArgsConstructor
public class AdminMedicineController {

    private final MedicineService medicineService;

    @PostMapping
    public ResponseEntity<MedicineResponse> add(@Valid @RequestBody MedicineRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicineService.add(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody MedicineRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(medicineService.update(id, authentication.getName(), request));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<MedicineResponse> updateStock(@PathVariable Long id,
                                                        @Valid @RequestBody StockUpdateRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(
                medicineService.updateStock(id, authentication.getName(), request.quantity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        medicineService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<MedicineResponse>> alerts() {
        return ResponseEntity.ok(medicineService.getAlerts());
    }
}