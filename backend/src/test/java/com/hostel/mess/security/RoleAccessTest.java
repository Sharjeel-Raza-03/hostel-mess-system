package com.hostel.mess.security;

import com.hostel.mess.config.SecurityConfig;
import com.hostel.mess.controller.AdminController;
import com.hostel.mess.controller.BillController;
import com.hostel.mess.model.Role;
import com.hostel.mess.service.BillingService;
import com.hostel.mess.service.LeaveService;
import com.hostel.mess.service.MealService;
import com.hostel.mess.service.MessCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, JwtUtil.class, AdminController.class, BillController.class})
@TestPropertySource(properties = {
        "JWT_SECRET=unit-test-only-secret-0123456789-abcdefghijklmnopqrstuvwxyz",
        "JWT_EXPIRATION_MS=3600000"
})
class RoleAccessTest {

    /** Gives this test its own minimal configuration, so it does not depend on finding MessApplication. */
    @SpringBootConfiguration
    static class TestApplication {
    }

    private static final String STUDENT_EMAIL = "student@test.com";
    private static final String ADMIN_EMAIL = "admin@hostel.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;

    @MockitoBean private CustomUserDetailsService userDetailsService;
    @MockitoBean private LeaveService leaveService;
    @MockitoBean private MealService mealService;
    @MockitoBean private MessCountService messCountService;
    @MockitoBean private BillingService billingService;

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(STUDENT_EMAIL))
                .thenReturn(userDetails(STUDENT_EMAIL, "ROLE_STUDENT"));
        when(userDetailsService.loadUserByUsername(ADMIN_EMAIL))
                .thenReturn(userDetails(ADMIN_EMAIL, "ROLE_ADMIN"));
    }

    private static UserDetails userDetails(String email, String authority) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email).password("not-used").authorities(authority).build();
    }

    private String bearer(String email, Role role) {
        return "Bearer " + jwtUtil.generateToken(email, role);
    }

    /** Changes the first character of the signature, so the token no longer matches its signature. */
    private String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char first = token.charAt(lastDot + 1);
        char replacement = first == 'A' ? 'B' : 'A';
        return token.substring(0, lastDot + 1) + replacement + token.substring(lastDot + 2);
    }

    @Test
    void adminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/leaves"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withTamperedToken_returns401() throws Exception {
        String tampered = tamper(jwtUtil.generateToken(ADMIN_EMAIL, Role.ADMIN));
        mockMvc.perform(get("/api/admin/leaves").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/leaves").header("Authorization", bearer(STUDENT_EMAIL, Role.STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/leaves").header("Authorization", bearer(ADMIN_EMAIL, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void studentBills_asStudent_returns200() throws Exception {
        mockMvc.perform(get("/api/bills/my").header("Authorization", bearer(STUDENT_EMAIL, Role.STUDENT)))
                .andExpect(status().isOk());
    }

    @Test
    void studentBills_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/bills/my").header("Authorization", bearer(ADMIN_EMAIL, Role.ADMIN)))
                .andExpect(status().isForbidden());
    }
}