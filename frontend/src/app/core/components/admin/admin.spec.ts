import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Admin } from './admin';

describe('Admin', () => {
  it('does not replace an unavailable stat with zero', () => {
    const adminService = adminServiceMock({
      getMatchs: vi.fn().mockReturnValue(throwError(() => backendError('Dashboard indisponible'))),
    });
    const component = new Admin(adminService as any, authServiceMock() as any, cdrMock() as any);

    component.ngOnInit();

    expect(component.matchs).toBeNull();
    expect(component.statErrors.matchs).toBe('Dashboard indisponible');
  });

  it('keeps previous dashboard values when a reload fails', () => {
    const adminService = adminServiceMock({
      getCA: vi.fn().mockReturnValue(throwError(() => backendError('CA indisponible'))),
    });
    const component = new Admin(adminService as any, authServiceMock() as any, cdrMock() as any);
    component.ca = 300;

    component.retryStat('ca');

    expect(component.ca).toBe(300);
    expect(component.statErrors.ca).toBe('CA indisponible');
  });

  it('tracks loading for an individual stat', () => {
    const matchs$ = new Subject<number>();
    const adminService = adminServiceMock({
      getMatchs: vi.fn().mockReturnValue(matchs$),
    });
    const component = new Admin(adminService as any, authServiceMock() as any, cdrMock() as any);

    component.retryStat('matchs');

    expect(component.loadingStats.matchs).toBe(true);

    matchs$.next(4);
    matchs$.complete();

    expect(component.loadingStats.matchs).toBe(false);
    expect(component.matchs).toBe(4);
  });

  it('retry relaunches only the requested stat', () => {
    const adminService = adminServiceMock();
    const component = new Admin(adminService as any, authServiceMock() as any, cdrMock() as any);

    component.retryStat('terrains');

    expect(adminService.getTerrains).toHaveBeenCalledTimes(1);
    expect(adminService.getMatchs).not.toHaveBeenCalled();
  });
});

function adminServiceMock(overrides: Partial<Record<string, any>> = {}) {
  return {
    getMatchs: vi.fn().mockReturnValue(of(2)),
    getCA: vi.fn().mockReturnValue(of(150)),
    getMembres: vi.fn().mockReturnValue(of(12)),
    getTerrains: vi.fn().mockReturnValue(of(3)),
    getTauxRemplissage: vi.fn().mockReturnValue(of(75)),
    getRevenusParSite: vi.fn().mockReturnValue(of({ Bruxelles: 150 })),
    getSites: vi.fn().mockReturnValue(of([{ id: 1, name: 'Bruxelles' }])),
    ...overrides,
  };
}

function authServiceMock() {
  return {
    isAdmin: vi.fn().mockReturnValue(true),
    getMatricule: vi.fn().mockReturnValue('G0001'),
  };
}

function cdrMock() {
  return { detectChanges: vi.fn() };
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
