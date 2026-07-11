import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AdminDashboard } from '../../models/admin-dashboard';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Admin } from './admin';

describe('Admin', () => {
  it('does not replace an unavailable dashboard with empty values', () => {
    const adminService = adminServiceMock({
      getDashboard: vi.fn().mockReturnValue(throwError(() => backendError('Dashboard indisponible'))),
    });
    const notificationService = notificationServiceMock();
    const component = new Admin(adminService, authServiceMock(), notificationService);

    component.ngOnInit();

    expect(component.dashboard).toBeNull();
    expect(component.error).toBe('Dashboard indisponible');
    expect(notificationService.error).toHaveBeenCalledWith('Dashboard indisponible');
  });

  it('keeps previous dashboard values when a reload fails', () => {
    const adminService = adminServiceMock({
      getDashboard: vi.fn().mockReturnValue(throwError(() => backendError('Dashboard indisponible'))),
    });
    const component = new Admin(adminService, authServiceMock(), notificationServiceMock());
    component.dashboard = dashboardFixture({ nombreMatchs: 4 });

    component.loadDashboard();

    expect(component.dashboard?.resume.nombreMatchs).toBe(4);
    expect(component.error).toBe('Dashboard indisponible');
  });

  it('tracks dashboard loading', () => {
    const dashboard$ = new Subject<AdminDashboard>();
    const adminService = adminServiceMock({
      getDashboard: vi.fn().mockReturnValue(dashboard$),
    });
    const component = new Admin(adminService, authServiceMock(), notificationServiceMock());

    component.loadDashboard();

    expect(component.loadingDashboard).toBe(true);

    dashboard$.next(dashboardFixture({ nombreMatchs: 6 }));
    dashboard$.complete();

    expect(component.loadingDashboard).toBe(false);
    expect(component.dashboard?.resume.nombreMatchs).toBe(6);
  });

  it('reloads terrains and dashboard when global admin changes site', () => {
    const adminService = adminServiceMock();
    const component = new Admin(adminService, authServiceMock(), notificationServiceMock());
    component.filters.siteId = 2;
    component.filters.terrainId = 10;

    component.onSiteChange();

    expect(component.filters.terrainId).toBeUndefined();
    expect(adminService.getTerrainsAccessibles).toHaveBeenCalledWith(2);
    expect(adminService.getDashboard).toHaveBeenCalled();
  });

});

function adminServiceMock(overrides: Partial<AdminService> = {}): AdminService {
  return {
    getMatchs: vi.fn().mockReturnValue(of(2)),
    getCA: vi.fn().mockReturnValue(of(150)),
    getMembres: vi.fn().mockReturnValue(of(12)),
    getTerrains: vi.fn().mockReturnValue(of(3)),
    getTauxRemplissage: vi.fn().mockReturnValue(of(75)),
    getRevenusParSite: vi.fn().mockReturnValue(of({ Bruxelles: 150 })),
    getSites: vi.fn().mockReturnValue(of([{ id: 1, name: 'Bruxelles', heureOuverture: '08:00', heureFermeture: '20:00' }])),
    getTerrainsAccessibles: vi.fn().mockReturnValue(of([{ id: 10, nom: 'T1', heureOuverture: '08:00', heureFermeture: '20:00' }])),
    getDashboard: vi.fn().mockReturnValue(of(dashboardFixture())),
    ...overrides,
  } as unknown as AdminService;
}

function authServiceMock(): AuthService {
  return {
    isAdmin: vi.fn().mockReturnValue(true),
    isAdminGlobal: vi.fn().mockReturnValue(true),
    getMatricule: vi.fn().mockReturnValue('G0001'),
  } as unknown as AuthService;
}

function notificationServiceMock(): NotificationService {
  return { error: vi.fn() } as unknown as NotificationService;
}

function backendError(message: string): HttpErrorResponse {
  return new HttpErrorResponse({
    status: 500,
    error: {
      status: 500,
      message,
      timestamp: '2026-07-10T18:00:00Z',
      fieldErrors: {},
    },
  });
}

function dashboardFixture(summary: Partial<AdminDashboard['resume']> = {}): AdminDashboard {
  return {
    resume: {
      chiffreAffaires: 150,
      nombreMatchs: 2,
      reservationsConfirmees: 3,
      tauxRemplissageMatchs: 50,
      tauxOccupationTerrains: 10,
      soldesDus: 20,
      membresActifs: 4,
      matchsAnnules: 1,
      matchsProchainsIncomplets: 1,
      ...summary,
    },
    chiffreAffairesParMois: [],
    matchsParMois: [],
    tauxRemplissageParTerrain: [],
    repartitionMatchsParStatut: [],
    terrainsLesPlusUtilises: [],
    prochainsMatchsIncomplets: [],
  };
}
