package be.ephec.pdw.padel;

import be.ephec.pdw.padel.controllers.AdminController;
import be.ephec.pdw.padel.dto.AdminDashboardDto;
import be.ephec.pdw.padel.dto.DashboardSummaryDto;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.service.AdminService;
import be.ephec.pdw.padel.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {
    private AdminService adminService;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        adminService = mock(AdminService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        MembreGlobal admin = new MembreGlobal();
        admin.setMatricule("G0001");
        admin.setRole(Role.ADMIN_GLOBAL);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(adminService, currentUserService))
                .build();
    }

    @Test
    void shouldReturnDashboardWithFilters() throws Exception {
        when(adminService.dashboard(eq("G0001"), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31)), eq(1L), eq(10L)))
                .thenReturn(emptyDashboard());

        mockMvc.perform(get("/admin/dashboard")
                        .param("dateDebut", "2026-01-01")
                        .param("dateFin", "2026-01-31")
                        .param("siteId", "1")
                        .param("terrainId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume.chiffreAffaires").value(0))
                .andExpect(jsonPath("$.resume.nombreMatchs").value(0));

        verify(adminService).dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1L, 10L);
    }

    private AdminDashboardDto emptyDashboard() {
        return new AdminDashboardDto(
                new DashboardSummaryDto(
                        BigDecimal.ZERO,
                        0,
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        0,
                        0
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
