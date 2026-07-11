package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.AdminDashboardDto;
import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {
    @Mock private MatchRepository matchRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private SiteRepository siteRepository;

    private AdminService adminService;
    private Site site;

    @BeforeEach
    void setup() {
        adminService = new AdminService(matchRepository, reservationRepository, membreRepository, terrainRepository, siteRepository);
        site = Site.builder()
                .id(1L)
                .name("Bruxelles")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(20, 0))
                .jourFermeture(List.of())
                .build();
        Terrain terrain = Terrain.builder().id(10L).nom("T1").site(site).build();
        site.setTerrains(List.of(terrain));

        when(membreRepository.findById("G0001")).thenReturn(Optional.of(globalAdmin()));
        when(membreRepository.findById("S0001")).thenReturn(Optional.of(siteAdmin(site)));
        when(siteRepository.findSitesForDashboard(any())).thenReturn(List.of(site));
        when(terrainRepository.findById(10L)).thenReturn(Optional.of(terrain));
        when(reservationRepository.sumPaidRevenue(any(), any(), any(), any())).thenReturn(0.0);
        when(membreRepository.sumAllSoldesDus()).thenReturn(0.0);
        when(membreRepository.sumSoldesDusBySite(any())).thenReturn(0.0);
        when(matchRepository.countMatchesByStatus(any(), any(), any(), any())).thenReturn(List.of());
        when(matchRepository.countMatchesByMonth(any(), any(), any(), any())).thenReturn(List.of());
        when(matchRepository.terrainStatistics(any(), any(), any(), any())).thenReturn(List.of());
        when(matchRepository.upcomingIncompleteMatches(any(), any(), any())).thenReturn(List.of());
        when(reservationRepository.sumRevenueByMonth(any(), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void shouldUseRequestedPeriod() {
        adminService.dashboard("G0001", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 20), null, null);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(matchRepository).countDashboardMatches(startCaptor.capture(), endCaptor.capture(), eq(null), eq(null));

        assertEquals(LocalDateTime.of(2026, 1, 5, 0, 0), startCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 1, 21, 0, 0), endCaptor.getValue());
    }

    @Test
    void shouldRejectSiteAdminTryingAnotherSite() {
        assertThrows(ForbiddenException.class, () ->
                adminService.dashboard("S0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 2L, null)
        );
    }

    @Test
    void shouldAllowGlobalAdminToFilterBySite() {
        adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1L, null);

        verify(matchRepository).countDashboardMatches(any(), any(), eq(1L), eq(null));
    }

    @Test
    void shouldApplyTerrainFilter() {
        adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 1L, 10L);

        verify(matchRepository).countDashboardMatches(any(), any(), eq(1L), eq(10L));
        verify(terrainRepository).findById(10L);
    }

    @Test
    void shouldCalculateRevenue() {
        when(reservationRepository.sumPaidRevenue(any(), any(), any(), any())).thenReturn(75.50);

        AdminDashboardDto dashboard = adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null);

        assertEquals(new BigDecimal("75.50"), dashboard.resume().chiffreAffaires());
    }

    @Test
    void shouldCalculateFillRate() {
        when(matchRepository.countDashboardMatches(any(), any(), any(), any())).thenReturn(2L);
        when(reservationRepository.countValidReservationsForFillRate(any(), any(), any(), any())).thenReturn(5L);

        AdminDashboardDto dashboard = adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null);

        assertEquals(new BigDecimal("62.50"), dashboard.resume().tauxRemplissageMatchs());
    }

    @Test
    void shouldReturnZeroRatesWhenThereIsNoMatch() {
        when(matchRepository.countDashboardMatches(any(), any(), any(), any())).thenReturn(0L);
        when(reservationRepository.countValidReservationsForFillRate(any(), any(), any(), any())).thenReturn(0L);

        AdminDashboardDto dashboard = adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null);

        assertEquals(BigDecimal.ZERO, dashboard.resume().tauxRemplissageMatchs());
    }

    @Test
    void shouldReturnZeroOccupationWhenNoSlotIsAvailable() {
        site.setHeureOuverture(LocalTime.of(8, 0));
        site.setHeureFermeture(LocalTime.of(8, 30));
        when(matchRepository.countUsedSlots(any(), any(), any(), any())).thenReturn(3L);

        AdminDashboardDto dashboard = adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), null, null);

        assertEquals(BigDecimal.ZERO, dashboard.resume().tauxOccupationTerrains());
    }

    @Test
    void shouldCalculateOccupationWithoutDivisionByZero() {
        site.setTerrains(List.of());
        when(matchRepository.countUsedSlots(any(), any(), any(), any())).thenReturn(1L);

        AdminDashboardDto dashboard = adminService.dashboard("G0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), null, null);

        assertEquals(BigDecimal.ZERO, dashboard.resume().tauxOccupationTerrains());
    }

    private MembreGlobal globalAdmin() {
        MembreGlobal admin = new MembreGlobal();
        admin.setMatricule("G0001");
        admin.setRole(Role.ADMIN_GLOBAL);
        return admin;
    }

    private MembreSite siteAdmin(Site adminSite) {
        MembreSite admin = new MembreSite();
        admin.setMatricule("S0001");
        admin.setRole(Role.ADMIN_SITE);
        admin.setSite(adminSite);
        return admin;
    }
}
