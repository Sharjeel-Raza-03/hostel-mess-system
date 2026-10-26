package com.hostel.mess.service;

import com.hostel.mess.dto.MedicineRequest;
import com.hostel.mess.dto.MedicineResponse;
import com.hostel.mess.exception.ConflictException;
import com.hostel.mess.model.Medicine;
import com.hostel.mess.model.MedicineStatus;
import com.hostel.mess.model.Setting;
import com.hostel.mess.repository.MedicineRepository;
import com.hostel.mess.repository.SettingRepository;
import com.hostel.mess.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);
    private static final int THRESHOLD = 10;

    @Mock private MedicineRepository medicineRepository;
    @Mock private SettingRepository settingRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private MedicineService medicineService;

    // ---------- status calculation (rule 14) ----------

    @Test
    void status_stockAboveThreshold_isAvailable() {
        assertEquals(MedicineStatus.AVAILABLE,
                MedicineService.computeStatus(50, TODAY.plusDays(100), TODAY, THRESHOLD));
    }

    @Test
    void status_stockBelowThreshold_isLowStock() {
        assertEquals(MedicineStatus.LOW_STOCK,
                MedicineService.computeStatus(5, TODAY.plusDays(100), TODAY, THRESHOLD));
    }

    @Test
    void status_stockExactlyAtThreshold_isLowStock() {
        assertEquals(MedicineStatus.LOW_STOCK,
                MedicineService.computeStatus(10, TODAY.plusDays(100), TODAY, THRESHOLD));
    }

    @Test
    void status_stockJustAboveThreshold_isAvailable() {
        assertEquals(MedicineStatus.AVAILABLE,
                MedicineService.computeStatus(11, TODAY.plusDays(100), TODAY, THRESHOLD));
    }

    @Test
    void status_zeroStock_isOutOfStock() {
        assertEquals(MedicineStatus.OUT_OF_STOCK,
                MedicineService.computeStatus(0, TODAY.plusDays(100), TODAY, THRESHOLD));
    }

    @Test
    void status_expiredYesterday_isExpiredEvenWithStock() {
        assertEquals(MedicineStatus.EXPIRED,
                MedicineService.computeStatus(50, TODAY.minusDays(1), TODAY, THRESHOLD));
    }

    @Test
    void status_expiringToday_isNotExpiredYet() {
        assertEquals(MedicineStatus.AVAILABLE,
                MedicineService.computeStatus(50, TODAY, TODAY, THRESHOLD));
    }

    @Test
    void status_expiredAndOutOfStock_isExpired() {
        assertEquals(MedicineStatus.EXPIRED,
                MedicineService.computeStatus(0, TODAY.minusDays(30), TODAY, THRESHOLD));
    }

    // ---------- service behaviour ----------

    @Test
    void add_sameNameAndExpiry_isRejectedWithConflict() {
        LocalDate expiry = LocalDate.of(2027, 12, 31);
        when(medicineRepository.existsByNameIgnoreCaseAndExpiryDate("Panadol", expiry)).thenReturn(true);

        MedicineRequest request = new MedicineRequest("Panadol", "Pain relief", "Tablets", 20, "strips", expiry);

        assertThrows(ConflictException.class, () -> medicineService.add("admin@hostel.local", request));
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    void search_withoutFilters_passesEmptyStringsInsteadOfNull() {
        when(settingRepository.findBySettingKeyIn(anyCollection())).thenReturn(List.of());
        when(medicineRepository.search(eq(""), eq(""), eq(false), any(LocalDate.class))).thenReturn(List.of());

        assertTrue(medicineService.search(null, null, false).isEmpty());
        verify(medicineRepository).search(eq(""), eq(""), eq(false), any(LocalDate.class));
    }

    @Test
    void search_readsTheLowStockThresholdFromSettings() {
        Medicine medicine = Medicine.builder().id(1L).name("Cetirizine").category("Allergy")
                .quantity(15).unit("tablets").expiryDate(LocalDate.now().plusYears(1)).build();
        Setting threshold = Setting.builder().settingKey("LOW_STOCK_THRESHOLD").settingValue("20").build();

        when(settingRepository.findBySettingKeyIn(anyCollection())).thenReturn(List.of(threshold));
        when(medicineRepository.search(eq(""), eq(""), eq(false), any(LocalDate.class)))
                .thenReturn(List.of(medicine));

        List<MedicineResponse> result = medicineService.search("", "", false);

        assertEquals(1, result.size());
        assertEquals(MedicineStatus.LOW_STOCK, result.get(0).status());
    }
}