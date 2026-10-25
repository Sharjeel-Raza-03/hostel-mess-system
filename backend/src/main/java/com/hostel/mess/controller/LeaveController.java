package com.hostel.mess.controller;

import com.hostel.mess.dto.LeaveCreateRequest;
import com.hostel.mess.dto.LeaveResponse;
import com.hostel.mess.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<LeaveResponse> create(@Valid @RequestBody LeaveCreateRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.create(authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveResponse>> myLeaves(Authentication authentication) {
        return ResponseEntity.ok(leaveService.getMyLeaves(authentication.getName()));
    }
}