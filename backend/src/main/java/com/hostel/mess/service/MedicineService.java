package com.hostel.mess.service;

import com.hostel.mess.dto.MedicineRequest;
import com.hostel.mess.dto.MedicineResponse;
import com.hostel.mess.exception.ConflictException;
import com.hostel.mess.exception.ResourceNotFoundException;
import com.hostel.mess.model.Medicine;
import com.hostel.mess.model.MedicineStatus;
import com.hostel.mess.model.Setting;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.MedicineRepository;
import com.hostel.mess.repository.SettingRepository;
import com.hostel.mess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private static final String THRESHOLD_KEY = "LOW_STOCK_THRESHOLD";
    private static final int DEFAULT_THRESHOLD = 10;

    private final MedicineRepository medicineRepository;
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;

    /** Rule 14: status is computed, never stored. Expired wins over the stock-based statuses. */
    public static MedicineStatus computeStatus(int quantity, LocalDate expiryDate,
                                               LocalDate today, int threshold) {
        if (expiryDate.isBefore(today)) {
            return MedicineStatus.EXPIRED;
        }
        if (quantity <= 0) {
            return MedicineStatus.OUT_OF_STOCK;
        }
        if (quantity <= threshold) {
            return MedicineStatus.LOW_STOCK;
        }
        return MedicineStatus.AVAILABLE;
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> search(String search, String category, boolean availableOnly) {
        String s = search == null ? "" : search.trim();
        String c = category == null ? "" : category.trim();
        LocalDate today = LocalDate.now();
        int threshold = getThreshold();
        return medicineRepository.search(s, c, availableOnly, today).stream()
                .map(m -> toResponse(m, today, threshold)).toList();
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return medicineRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public MedicineResponse getById(Long id) {
        return toResponse(find(id), LocalDate.now(), getThreshold());
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> getAlerts() {
        LocalDate today = LocalDate.now();
        int threshold = getThreshold();
        return medicineRepository.findAlerts(today, threshold).stream()
                .map(m -> toResponse(m, today, threshold)).toList();
    }

    @Transactional
    public MedicineResponse add(String adminEmail, MedicineRequest request) {
        String name = request.name().trim();
        if (medicineRepository.existsByNameIgnoreCaseAndExpiryDate(name, request.expiryDate())) {
            throw new ConflictException("This medicine batch already exists");
        }

        Medicine m = Medicine.builder()
                .name(name)
                .category(clean(request.category()))
                .description(clean(request.description()))
                .quantity(request.quantity())
                .unit(clean(request.unit()))
                .expiryDate(request.expiryDate())
                .updatedBy(findUser(adminEmail))
                .build();

        return toResponse(medicineRepository.save(m), LocalDate.now(), getThreshold());
    }

    @Transactional
    public MedicineResponse update(Long id, String adminEmail, MedicineRequest request) {
        Medicine m = find(id);
        String name = request.name().trim();

        boolean keyChanged = !m.getName().equalsIgnoreCase(name)
                || !m.getExpiryDate().equals(request.expiryDate());
        if (keyChanged && medicineRepository.existsByNameIgnoreCaseAndExpiryDate(name, request.expiryDate())) {
            throw new ConflictException("This medicine batch already exists");
        }

        m.setName(name);
        m.setCategory(clean(request.category()));
        m.setDescription(clean(request.description()));
        m.setQuantity(request.quantity());
        m.setUnit(clean(request.unit()));
        m.setExpiryDate(request.expiryDate());
        m.setUpdatedBy(findUser(adminEmail));

        return toResponse(medicineRepository.save(m), LocalDate.now(), getThreshold());
    }

    @Transactional
    public MedicineResponse updateStock(Long id, String adminEmail, int quantity) {
        Medicine m = find(id);
        m.setQuantity(quantity);
        m.setUpdatedBy(findUser(adminEmail));
        return toResponse(medicineRepository.save(m), LocalDate.now(), getThreshold());
    }

    @Transactional
    public void delete(Long id) {
        medicineRepository.delete(find(id));
    }

    private Medicine find(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private int getThreshold() {
        return settingRepository.findBySettingKeyIn(List.of(THRESHOLD_KEY)).stream()
                .findFirst()
                .map(Setting::getSettingValue)
                .map(v -> {
                    try {
                        return Integer.parseInt(v.trim());
                    } catch (NumberFormatException ex) {
                        return DEFAULT_THRESHOLD;
                    }
                })
                .orElse(DEFAULT_THRESHOLD);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private MedicineResponse toResponse(Medicine m, LocalDate today, int threshold) {
        return new MedicineResponse(m.getId(), m.getName(), m.getCategory(), m.getDescription(),
                m.getQuantity(), m.getUnit(), m.getExpiryDate(),
                computeStatus(m.getQuantity(), m.getExpiryDate(), today, threshold),
                m.getUpdatedAt());
    }
}