import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { MesReservations } from './mes-reservations';

describe('MesReservations', () => {
  it('shows and hides the loader while reservations are loading', () => {
    const reservations$ = new Subject<any[]>();
    const component = createComponent({
      reservationService: { getMesReservations: vi.fn().mockReturnValue(reservations$) },
    });

    component.loadReservations();

    expect(component.loading()).toBe(true);

    reservations$.next([]);
    reservations$.complete();

    expect(component.loading()).toBe(false);
    expect(component.reservations()).toEqual([]);
  });

  it('keeps existing reservations when loading fails', () => {
    const component = createComponent({
      reservationService: {
        getMesReservations: vi.fn().mockReturnValue(throwError(() => ({
          error: {
            status: 503,
            message: 'Réservations indisponibles',
            timestamp: '2026-07-10T18:00:00Z',
            fieldErrors: {},
          },
        }))),
      },
    });
    component.reservations.set([{ id: 1, match: { dateHeureDebut: futureDate() } }]);

    component.loadReservations();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Réservations indisponibles');
    expect(component.reservations().length).toBe(1);
  });

  it('prevents double payment submissions and shows success', () => {
    const payment$ = new Subject<object>();
    const reservationService = {
      getMesReservations: vi.fn().mockReturnValue(of([])),
      payerReservation: vi.fn().mockReturnValue(payment$),
      ajouterJoueurMatchPrive: vi.fn(),
    };
    const component = createComponent({ reservationService });

    component.payerReservation(3);
    component.payerReservation(3);

    expect(reservationService.payerReservation).toHaveBeenCalledTimes(1);
    expect(component.payingReservationId()).toBe(3);

    payment$.next({});
    payment$.complete();

    expect(component.payingReservationId()).toBeNull();
    expect(component.success()).toBe('Le paiement a été enregistré.');
  });

  it('shows backend error after payment failure', () => {
    const component = createComponent({
      reservationService: {
        getMesReservations: vi.fn().mockReturnValue(of([])),
        payerReservation: vi.fn().mockReturnValue(throwError(() => ({
          error: {
            status: 400,
            message: 'Vous ne pouvez pas payer cette réservation',
            timestamp: '2026-07-10T18:00:00Z',
            fieldErrors: {},
          },
        }))),
        ajouterJoueurMatchPrive: vi.fn(),
      },
    });

    component.payerReservation(3);

    expect(component.payingReservationId()).toBeNull();
    expect(component.error()).toBe('Vous ne pouvez pas payer cette réservation');
  });
});

function createComponent(overrides: { reservationService?: any; authService?: any } = {}) {
  const reservationService = {
    getMesReservations: vi.fn().mockReturnValue(of([])),
    payerReservation: vi.fn().mockReturnValue(of({})),
    ajouterJoueurMatchPrive: vi.fn().mockReturnValue(of({})),
    ...overrides.reservationService,
  };
  const authService = {
    getMatricule: vi.fn().mockReturnValue('G0002'),
    ...overrides.authService,
  };

  return new MesReservations(reservationService as any, authService as any);
}

function futureDate(): string {
  return new Date(Date.now() + 60_000).toISOString();
}
