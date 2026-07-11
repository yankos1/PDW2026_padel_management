import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { MatchListComponent } from './match-list.component';

describe('MatchListComponent', () => {
  it('shows and hides the loader while matches are loading', () => {
    const matchs$ = new Subject<any[]>();
    const reservations$ = new Subject<any[]>();
    const component = createComponent({
      matchService: { getMatchDisponibles: vi.fn().mockReturnValue(matchs$) },
      reservationService: { getMesReservations: vi.fn().mockReturnValue(reservations$) },
    });

    component.loadMatchs();

    expect(component.loading()).toBe(true);

    matchs$.next([]);
    matchs$.complete();
    reservations$.next([]);
    reservations$.complete();

    expect(component.loading()).toBe(false);
    expect(component.matchs()).toEqual([]);
  });

  it('hides the loader and shows a backend message after an error', () => {
    const component = createComponent({
      reservationService: {
        getMesReservations: vi.fn().mockReturnValue(throwError(() => ({
          error: {
            status: 401,
            message: 'Authentification requise',
            timestamp: '2026-07-10T18:00:00Z',
            fieldErrors: {},
          },
        }))),
      },
    });

    component.loadMatchs();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Authentification requise');
  });

  it('filters already reserved matches and exposes an empty state', () => {
    const component = createComponent({
      matchService: {
        getMatchDisponibles: vi.fn().mockReturnValue(of([
          { id: 1, dateHeureDebut: futureDate() },
        ])),
      },
      reservationService: {
        getMesReservations: vi.fn().mockReturnValue(of([{ match: { id: 1 } }])),
      },
    });

    component.loadMatchs();

    expect(component.matchs()).toEqual([]);
    expect(component.error()).toBeNull();
  });

  it('prevents double reservation submissions and shows success notification', () => {
    const join$ = new Subject<any>();
    const notificationService = notificationMock();
    const reservationService = {
      getMesReservations: vi.fn().mockReturnValue(of([])),
      rejoindreMatch: vi.fn().mockReturnValue(join$),
      payerReservation: vi.fn(),
    };
    const component = createComponent({ reservationService, notificationService });
    const match = { id: 7, dateHeureDebut: futureDate(), siteId: 1 };

    component.rejoindreMatch(match);
    component.rejoindreMatch(match);

    expect(reservationService.rejoindreMatch).toHaveBeenCalledTimes(1);
    expect(component.joiningMatchId()).toBe(7);

    join$.next({ id: 22 });
    join$.complete();

    expect(component.joiningMatchId()).toBeNull();
    expect(notificationService.success).toHaveBeenCalledWith('Réservation en attente de paiement.');
  });

  it('filters matches case-insensitively and trims spaces', () => {
    const component = createComponent();
    component.matchs.set([
      { id: 1, site: 'Bruxelles', terrain: 'Central', dateHeureDebut: futureDate(), statut: 'PLANIFIE', estPublic: true },
      { id: 2, site: 'Namur', terrain: 'Court 2', dateHeureDebut: futureDate(), statut: 'COMPLET', estPublic: true },
    ]);

    component.searchTerm = '  brux ';

    expect(component.filteredMatchs().map((match) => match.id)).toEqual([1]);
  });

  it('resets match filters', () => {
    const component = createComponent();
    component.searchTerm = 'namur';
    component.statutFilter = 'COMPLET';
    component.typeFilter = 'PUBLIC';

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.statutFilter).toBe('');
    expect(component.typeFilter).toBe('');
  });

  it('waits for payment confirmation before triggering the action', () => {
    const dialog = dialogMock(false);
    const component = createComponent({ dialog });
    const payer = vi.spyOn(component, 'payerMatchPublic');

    component.ouvrirPaiement({ id: 7, dateHeureDebut: futureDate(), siteId: 1 });

    expect(payer).not.toHaveBeenCalled();
  });

  it('starts public payment after confirmation', () => {
    const dialog = dialogMock(true);
    const component = createComponent({ dialog });
    const payer = vi.spyOn(component, 'payerMatchPublic');

    component.ouvrirPaiement({ id: 7, dateHeureDebut: futureDate(), siteId: 1 });

    expect(payer).toHaveBeenCalledTimes(1);
  });
});

function createComponent(overrides: {
  matchService?: any;
  reservationService?: any;
  authService?: any;
  notificationService?: any;
  dialog?: any;
} = {}) {
  const matchService = {
    getMatchDisponibles: vi.fn().mockReturnValue(of([])),
    ...overrides.matchService,
  };
  const reservationService = {
    getMesReservations: vi.fn().mockReturnValue(of([])),
    rejoindreMatch: vi.fn().mockReturnValue(of({ id: 1 })),
    payerReservation: vi.fn().mockReturnValue(of({})),
    ...overrides.reservationService,
  };
  const authService = {
    getMatricule: vi.fn().mockReturnValue('G0002'),
    canReserveDate: vi.fn().mockReturnValue(true),
    getDelaiReservation: vi.fn().mockReturnValue(21),
    getSiteMembreId: vi.fn().mockReturnValue(null),
    getTypeMembre: vi.fn().mockReturnValue('GLOBAL'),
    ...overrides.authService,
  };

  return new MatchListComponent(
    matchService as any,
    reservationService as any,
    authService as any,
    (overrides.notificationService ?? notificationMock()) as any,
    (overrides.dialog ?? dialogMock(false)) as any,
  );
}

function futureDate(): string {
  return new Date(Date.now() + 60_000).toISOString();
}

function notificationMock() {
  return {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  };
}

function dialogMock(confirmed: boolean) {
  return {
    open: vi.fn().mockReturnValue({
      afterClosed: () => of(confirmed),
    }),
  };
}
