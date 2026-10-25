package com.hostel.mess.service;

import com.hostel.mess.dto.LeaveCreateRequest;
import com.hostel.mess.dto.LeaveResponse;
import com.hostel.mess.dto.ReviewRequest;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.exception.ConflictException;
import com.hostel.mess.exception.ResourceNotFoundException;
import com.hostel.mess.model.LeaveRequest;
import com.hostel.mess.model.LeaveStatus;
import com.hostel.mess.model.MealRecord;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.LeaveRequestRepository;
import com.hostel.mess.repository.MealRecordRepository;
import com.hostel.mess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final long MAX_LEAVE_DAYS = 90;

    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;
    private final MealRecordRepository mealRepository;
    private final MessCountService messCountService;
    private final BillingService billingService;

    @Transactional
    public LeaveResponse create(String email, LeaveCreateRequest request) {
        if (request.toDate().isBefore(request.fromDate())) {
            throw new BadRequestException("To date cannot be before from date");
        }
        if (request.fromDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("From date cannot be in the past");
        }
        if (ChronoUnit.DAYS.between(request.fromDate(), request.toDate()) + 1 > MAX_LEAVE_DAYS) {
            throw new BadRequestException("A leave request cannot be longer than " + MAX_LEAVE_DAYS + " days");
        }

        User user = findUser(email);
        if (leaveRepository.hasOverlap(user.getId(), request.fromDate(), request.toDate())) {
            throw new ConflictException("You already have a pending or approved leave overlapping these dates");
        }

        LeaveRequest leave = LeaveRequest.builder()
                .user(user)
                .fromDate(request.fromDate())
                .toDate(request.toDate())
                .reason(request.reason().trim())
                .status(LeaveStatus.PENDING)
                .build();

        return toResponse(leaveRepository.save(leave));
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(String email) {
        User user = findUser(email);
        return leaveRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getAll(LeaveStatus status) {
        List<LeaveRequest> list = status == null
                ? leaveRepository.findAllByOrderByCreatedAtDesc()
                : leaveRepository.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::toResponse).toList();
    }

    /**
     * Rule 7: one transaction. If anything fails, nothing is saved.
     * 1) status APPROVED  2) meals on leave  3) mess counts  4) bills for every affected month
     */
    @Transactional
    public LeaveResponse approve(Long id, String adminEmail, ReviewRequest review) {
        LeaveRequest leave = changeStatus(id, adminEmail, review, LeaveStatus.APPROVED);
        applyApprovalEffects(leave);
        return toResponse(leave);
    }

    @Transactional
    public LeaveResponse reject(Long id, String adminEmail, ReviewRequest review) {
        return toResponse(changeStatus(id, adminEmail, review, LeaveStatus.REJECTED));
    }

    private LeaveRequest changeStatus(Long id, String adminEmail, ReviewRequest review, LeaveStatus newStatus) {
        LeaveRequest leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("Only pending leave requests can be approved or rejected");
        }

        leave.setStatus(newStatus);
        leave.setReviewedBy(findUser(adminEmail));
        leave.setReviewedAt(LocalDateTime.now());
        if (review != null && review.remark() != null && !review.remark().isBlank()) {
            leave.setAdminRemark(review.remark().trim());
        }
        return leaveRepository.save(leave);
    }

    private void applyApprovalEffects(LeaveRequest leave) {
        User student = leave.getUser();

        for (LocalDate d = leave.getFromDate(); !d.isAfter(leave.getToDate()); d = d.plusDays(1)) {
            final LocalDate day = d;
            MealRecord record = mealRepository.findByUserIdAndMealDate(student.getId(), day)
                    .orElseGet(() -> MealRecord.builder().user(student).mealDate(day).build());
            record.setBreakfast(false);
            record.setLunch(false);
            record.setDinner(false);
            record.setOnLeave(true);
            mealRepository.save(record);

            messCountService.recalculate(day);
        }

        YearMonth start = YearMonth.from(leave.getFromDate());
        YearMonth end = YearMonth.from(leave.getToDate());
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            billingService.calculateAndSave(student, ym.getMonthValue(), ym.getYear());
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private LeaveResponse toResponse(LeaveRequest l) {
        User u = l.getUser();
        return new LeaveResponse(l.getId(), u.getId(), u.getFullName(), u.getRoomNo(),
                l.getFromDate(), l.getToDate(), l.getReason(), l.getStatus().name(),
                l.getAdminRemark(),
                l.getReviewedBy() != null ? l.getReviewedBy().getFullName() : null,
                l.getCreatedAt(), l.getReviewedAt());
    }
}