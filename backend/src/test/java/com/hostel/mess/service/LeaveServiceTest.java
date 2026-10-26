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
import com.hostel.mess.model.Role;
import com.hostel.mess.model.User;
import com.hostel.mess.repository.LeaveRequestRepository;
import com.hostel.mess.repository.MealRecordRepository;
import com.hostel.mess.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    private static final String STUDENT_EMAIL = "student@test.com";
    private static final String ADMIN_EMAIL = "admin@hostel.local";

    @Mock private LeaveRequestRepository leaveRepository;
    @Mock private UserRepository userRepository;
    @Mock private MealRecordRepository mealRepository;
    @Mock private MessCountService messCountService;
    @Mock private BillingService billingService;
    @InjectMocks private LeaveService leaveService;

    private static User student() {
        return User.builder().id(2L).fullName("Test Student").email(STUDENT_EMAIL)
                .role(Role.STUDENT).roomNo("A-101").build();
    }

    private static User admin() {
        return User.builder().id(1L).fullName("Hostel Admin").email(ADMIN_EMAIL)
                .role(Role.ADMIN).build();
    }

    private static LeaveRequest pendingLeave(long id, User user, LocalDate from, LocalDate to) {
        return LeaveRequest.builder().id(id).user(user).fromDate(from).toDate(to)
                .reason("Family event").status(LeaveStatus.PENDING).build();
    }

    // ---------- creating a leave request (rules 4 and 6) ----------

    @Test
    void create_toDateBeforeFromDate_isRejected() {
        LocalDate from = LocalDate.now().plusDays(5);
        LeaveCreateRequest request = new LeaveCreateRequest(from, from.minusDays(1), "Trip");

        assertThrows(BadRequestException.class, () -> leaveService.create(STUDENT_EMAIL, request));
        verifyNoInteractions(leaveRepository);
    }

    @Test
    void create_fromDateInThePast_isRejected() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LeaveCreateRequest request = new LeaveCreateRequest(yesterday, yesterday.plusDays(1), "Trip");

        assertThrows(BadRequestException.class, () -> leaveService.create(STUDENT_EMAIL, request));
        verifyNoInteractions(leaveRepository);
    }

    @Test
    void create_longerThan90Days_isRejected() {
        LocalDate from = LocalDate.now().plusDays(1);
        LeaveCreateRequest request = new LeaveCreateRequest(from, from.plusDays(100), "Long trip");

        assertThrows(BadRequestException.class, () -> leaveService.create(STUDENT_EMAIL, request));
        verifyNoInteractions(leaveRepository);
    }

    @Test
    void create_overlappingAnotherLeave_isRejectedWithConflict() {
        LocalDate from = LocalDate.now().plusDays(3);
        LocalDate to = from.plusDays(2);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student()));
        when(leaveRepository.hasOverlap(2L, from, to)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> leaveService.create(STUDENT_EMAIL, new LeaveCreateRequest(from, to, "Trip")));
        verify(leaveRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void create_validRequest_isSavedAsPending() {
        LocalDate from = LocalDate.now().plusDays(3);
        LocalDate to = from.plusDays(2);
        when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(student()));
        when(leaveRepository.hasOverlap(2L, from, to)).thenReturn(false);
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.create(STUDENT_EMAIL,
                new LeaveCreateRequest(from, to, "  Family event "));

        assertEquals("PENDING", response.status());
        assertEquals("Family event", response.reason());
        assertEquals(from, response.fromDate());
    }

    // ---------- approving a leave request (rule 7) ----------

    @Test
    void approve_putsEveryDayOnLeave_updatesCountsAndRecalculatesEachMonthsBill() {
        User student = student();
        LeaveRequest leave = pendingLeave(10L, student,
                LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2));

        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin()));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealRepository.findByUserIdAndMealDate(eq(2L), any(LocalDate.class))).thenReturn(Optional.empty());
        when(mealRepository.save(any(MealRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.approve(10L, ADMIN_EMAIL, new ReviewRequest("ok"));

        assertEquals("APPROVED", response.status());
        assertEquals("Hostel Admin", response.reviewedByName());

        ArgumentCaptor<MealRecord> captor = ArgumentCaptor.forClass(MealRecord.class);
        verify(mealRepository, times(4)).save(captor.capture());
        List<LocalDate> expectedDates = List.of(
                LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
        assertEquals(expectedDates, captor.getAllValues().stream().map(MealRecord::getMealDate).toList());
        captor.getAllValues().forEach(r -> {
            assertTrue(r.isOnLeave());
            assertFalse(r.isBreakfast());
            assertFalse(r.isLunch());
            assertFalse(r.isDinner());
        });

        expectedDates.forEach(d -> verify(messCountService).recalculate(d));
        verify(billingService).calculateAndSave(student, 10, 2026);
        verify(billingService).calculateAndSave(student, 11, 2026);
    }

    @Test
    void approve_clearsTheMealsOfAnExistingRecord() {
        User student = student();
        LocalDate day = LocalDate.of(2026, 12, 5);
        LeaveRequest leave = pendingLeave(11L, student, day, day);
        MealRecord existing = MealRecord.builder().user(student).mealDate(day)
                .breakfast(true).lunch(true).dinner(true).build();

        when(leaveRepository.findById(11L)).thenReturn(Optional.of(leave));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin()));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mealRepository.findByUserIdAndMealDate(2L, day)).thenReturn(Optional.of(existing));
        when(mealRepository.save(any(MealRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        leaveService.approve(11L, ADMIN_EMAIL, null);

        assertTrue(existing.isOnLeave());
        assertFalse(existing.isBreakfast());
        assertFalse(existing.isLunch());
        assertFalse(existing.isDinner());
        verify(mealRepository).save(existing);
        verify(billingService).calculateAndSave(student, 12, 2026);
    }

    @Test
    void approve_leaveThatIsNotPending_isRejectedWithConflict() {
        LeaveRequest leave = pendingLeave(12L, student(),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2));
        leave.setStatus(LeaveStatus.APPROVED);
        when(leaveRepository.findById(12L)).thenReturn(Optional.of(leave));

        assertThrows(ConflictException.class, () -> leaveService.approve(12L, ADMIN_EMAIL, null));
        verifyNoInteractions(mealRepository, messCountService, billingService);
    }

    @Test
    void approve_unknownLeave_isNotFound() {
        when(leaveRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.approve(404L, ADMIN_EMAIL, null));
    }

    // ---------- rejecting a leave request (rule 8) ----------

    @Test
    void reject_changesOnlyTheStatus_andLeavesMealsAndBillsAlone() {
        LeaveRequest leave = pendingLeave(13L, student(),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));
        when(leaveRepository.findById(13L)).thenReturn(Optional.of(leave));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin()));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.reject(13L, ADMIN_EMAIL, new ReviewRequest("Not enough notice"));

        assertEquals("REJECTED", response.status());
        assertEquals("Not enough notice", response.adminRemark());
        verifyNoInteractions(mealRepository, messCountService, billingService);
    }
}