import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CreateMatch } from './create-match';

describe('CreateMatch', () => {
  it('tracks site loading and hides the loader after success', () => {
    const sites$ = new Subject<any[]>();
    const matchService = matchServiceMock({ getSites: vi.fn().mockReturnValue(sites$) });
    const component = createComponent({ matchService });

    component.loadSites();

    expect(component.loadingSites()).toBe(true);

    sites$.next([{ id: 1, name: 'Bruxelles' }]);
    sites$.complete();

    expect(component.loadingSites()).toBe(false);
    expect(component.sites().length).toBe(1);
  });

  it('keeps form values and shows backend message when creation fails', () => {
    const matchService = matchServiceMock({
      createMatch: vi.fn().mockReturnValue(throwError(() => ({
        error: {
          status: 400,
          message: 'Vous ne pouvez pas réserver ce créneau',
          timestamp: '2026-07-10T18:00:00Z',
          fieldErrors: { terrainID: 'Terrain indisponible' },
        },
      }))),
    });
    const component = createReadyComponent(matchService);

    component.createMatch();

    expect(component.submitting()).toBe(false);
    expect(component.error()).toBe('Vous ne pouvez pas réserver ce créneau');
    expect(component.fieldError('terrainID')).toBe('Terrain indisponible');
    expect(component.date).toBe('2026-08-01');
  });

  it('prevents double creation submissions and shows success notification', () => {
    const create$ = new Subject<object>();
    const notificationService = notificationMock();
    const matchService = matchServiceMock({ createMatch: vi.fn().mockReturnValue(create$) });
    const component = createReadyComponent(matchService, notificationService);

    component.createMatch();
    component.createMatch();

    expect(matchService.createMatch).toHaveBeenCalledTimes(1);
    expect(component.submitting()).toBe(true);

    create$.next({});
    create$.complete();

    expect(component.submitting()).toBe(false);
    expect(notificationService.success).toHaveBeenCalledWith('Match créé avec succès.');
  });
});

function createReadyComponent(matchService = matchServiceMock(), notificationService = notificationMock()) {
  const component = createComponent({ matchService, notificationService });
  component.date = '2026-08-01';
  component.heureSelected = '10:00';
  component.terrainId = 4;
  component.siteId = 1;
  return component;
}

function createComponent(overrides: {
  matchService?: any;
  authService?: any;
  router?: any;
  notificationService?: any;
} = {}) {
  return new CreateMatch(
    (overrides.matchService ?? matchServiceMock()) as any,
    (overrides.router ?? { navigate: vi.fn() }) as any,
    (overrides.authService ?? authServiceMock()) as any,
    (overrides.notificationService ?? notificationMock()) as any,
  );
}

function matchServiceMock(overrides: Record<string, any> = {}) {
  return {
    getSites: vi.fn().mockReturnValue(of([])),
    createMatch: vi.fn().mockReturnValue(of({})),
    getCreneauxDisponibles: vi.fn().mockReturnValue(of([])),
    getTerrainsDisponiblesParCreneau: vi.fn().mockReturnValue(of([])),
    ...overrides,
  };
}

function authServiceMock() {
  return {
    getMatricule: vi.fn().mockReturnValue('G0001'),
    canReserveDate: vi.fn().mockReturnValue(true),
    getDateMinReservation: vi.fn().mockReturnValue('2026-07-11'),
    getDateMaxReservation: vi.fn().mockReturnValue('2026-12-31'),
    getTypeMembre: vi.fn().mockReturnValue('GLOBAL'),
    getSiteMembreId: vi.fn().mockReturnValue(null),
    getDelaiReservation: vi.fn().mockReturnValue(21),
  };
}

function notificationMock() {
  return {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  };
}
