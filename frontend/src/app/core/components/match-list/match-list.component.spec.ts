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

  it('prevents double reservation submissions and shows success', () => {
    const join$ = new Subject<any>();
    const reservationService = {
      getMesReservations: vi.fn().mockReturnValue(of([])),
      rejoindreMatch: vi.fn().mockReturnValue(join$),
      payerReservation: vi.fn(),
    };
    const component = createComponent({ reservationService });
    const match = { id: 7, dateHeureDebut: futureDate(), siteId: 1 };

    component.rejoindreMatch(match);
    component.rejoindreMatch(match);

    expect(reservationService.rejoindreMatch).toHaveBeenCalledTimes(1);
    expect(component.joiningMatchId()).toBe(7);

    join$.next({ id: 22 });
    join$.complete();

    expect(component.joiningMatchId()).toBeNull();
    expect(component.success()).toBe('Réservation en attente de paiement.');
  });
});

function createComponent(overrides: {
  matchService?: any;
  reservationService?: any;
  authService?: any;
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

  return new MatchListComponent(matchService as any, reservationService as any, authService as any);
}

function futureDate(): string {
  return new Date(Date.now() + 60_000).toISOString();
}
