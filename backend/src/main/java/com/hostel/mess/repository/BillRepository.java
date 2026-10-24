package com.hostel.mess.repository;

import com.hostel.mess.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByUserIdAndBillMonthAndBillYear(Long userId, Integer billMonth, Integer billYear);

    List<Bill> findByUserIdOrderByBillYearDescBillMonthDesc(Long userId);

    List<Bill> findByBillMonthAndBillYear(Integer billMonth, Integer billYear);
}