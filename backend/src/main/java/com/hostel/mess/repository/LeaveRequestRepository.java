package com.hostel.mess.repository;

import com.hostel.mess.model.LeaveRequest;
import com.hostel.mess.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    @Query("select count(l) > 0 from LeaveRequest l "
            + "where l.user.id = :userId "
            + "and l.status <> com.hostel.mess.model.LeaveStatus.REJECTED "
            + "and l.fromDate <= :toDate and l.toDate >= :fromDate")
    boolean hasOverlap(@Param("userId") Long userId,
                       @Param("fromDate") LocalDate fromDate,
                       @Param("toDate") LocalDate toDate);
}