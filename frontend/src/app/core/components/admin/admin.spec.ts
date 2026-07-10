import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Admin } from './admin';

describe('Admin', () => {
  it('keeps previous dashboard values and shows an error when loading fails', () => {
    const adminService = {
      getMatchs: vi.fn().mockReturnValue(throwError(() => backendError('Dashboard indisponible'))),
      getCA: vi.fn().mockReturnValue(of(150)),
      getMembres: vi.fn().mockReturnValue(of(12)),
      getTerrains: vi.fn().mockReturnValue(of(3)),
      getTauxRemplissage: vi.fn().mockReturnValue(of(75)),
      getRevenusParSite: vi.fn().mockReturnValue(of({ Bruxelles: 150 })),
      getSites: vi.fn().mockReturnValue(of([{ id: 1, name: 'Bruxelles' }])),
    };
    const authService = {
      isAdmin: vi.fn().mockReturnValue(true),
      getMatricule: vi.fn().mockReturnValue('G0001'),
    };
    const cdr = { detectChanges: vi.fn() };
    const component = new Admin(adminService as any, authService as any, cdr as any);
    component.matchs = 9;
    component.ca = 300;

    component.ngOnInit();

    expect(component.error).toBe('Dashboard indisponible');
    expect(component.loading).toBe(false);
    expect(component.matchs).toBe(9);
    expect(component.ca).toBe(300);
    expect(cdr.detectChanges).toHaveBeenCalled();
  });
});

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
