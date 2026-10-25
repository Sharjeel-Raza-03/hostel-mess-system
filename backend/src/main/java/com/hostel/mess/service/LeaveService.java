package com.hostel.mess.service;

import com.hostel.mess.dto.LeaveCreateRequest;
import com.hostel.mess.dto.LeaveResponse;
import com.hostel.mess.dto.ReviewRequest;
import com.hostel.mess.exception.BadRequestException;
import com.hostel.mess.exception.ConflictException;
import com.hostel.mess.exception.ResourceNotFoundException;
import com.hostel.mess.model.LeaveRequest;
import com.hostel.mess.model.LeaveStatus;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.LeaveRequestRepository;
import com.hostel.mess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;

    @Transactional
    public LeaveResponse create(String email, LeaveCreateRequest request) {
        if (request.toDate().isBefore(request.fromDate())) {
            throw new BadRequestException("To date cannot be before from date");
        }
        if (request.fromDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("From date cannot be in the past");
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

    /** Step 5 adds the meal, mess-count and bill changes on approval. */
    @Transactional
    public LeaveResponse approve(Long id, String adminEmail, ReviewRequest review) {
        return review(id, adminEmail, review, LeaveStatus.APPROVED);
    }

    @Transactional
    public LeaveResponse reject(Long id, String adminEmail, ReviewRequest review) {
        return review(id, adminEmail, review, LeaveStatus.REJECTED);
    }

    private LeaveResponse review(Long id, String adminEmail, ReviewRequest review, LeaveStatus newStatus) {
        LeaveRequest leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("Only pending leave requests can be approved or rejected");
        }

        User admin = findUser(adminEmail);
        leave.setStatus(newStatus);
        leave.setReviewedBy(admin);
        leave.setReviewedAt(LocalDateTime.now());
        if (review != null && review.remark() != null && !review.remark().isBlank()) {
            leave.setAdminRemark(review.remark().trim());
        }
        return toResponse(leaveRepository.save(leave));
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