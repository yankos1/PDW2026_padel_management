import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { MesReservations } from './mes-reservations';
import { Reservation } from '../../models/reservation';

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
    component.reservations.set([reservationFixture({ id: 1, dateHeureDebut: futureDate() })]);

    component.loadReservations();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Réservations indisponibles');
    expect(component.reservations().length).toBe(1);
  });

  it('prevents double payment submissions and shows success notification', () => {
    const payment$ = new Subject<object>();
    const notificationService = notificationMock();
    const reservationService = {
      getMesReservations: vi.fn().mockReturnValue(of([])),
      payerReservation: vi.fn().mockReturnValue(payment$),
      ajouterJoueurMatchPrive: vi.fn(),
    };
    const component = createComponent({ reservationService, notificationService });

    component.payerReservation(3);
    component.payerReservation(3);

    expect(reservationService.payerReservation).toHaveBeenCalledTimes(1);
    expect(component.payingReservationId()).toBe(3);

    payment$.next({});
    payment$.complete();

    expect(component.payingReservationId()).toBeNull();
    expect(notificationService.success).toHaveBeenCalledWith('Paiement enregistré avec succès.');
  });

  it('shows backend error after payment failure', () => {
    const notificationService = notificationMock();
    const component = createComponent({
      notificationService,
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
    expect(notificationService.error).toHaveBeenCalledWith('Vous ne pouvez pas payer cette réservation');
  });

  it('filters reservations case-insensitively and trims spaces', () => {
    const component = createComponent();
    component.reservations.set([
      reservationFixture({ id: 1, site: 'Bruxelles', terrain: 'Central', estPayee: true }),
      reservationFixture({ id: 2, site: 'Namur', terrain: 'Court 2', estPayee: false }),
    ]);

    component.searchTerm = '  NAM ';

    expect(component.filteredReservations().map((reservation) => reservation.id)).toEqual([2]);
  });

  it('shows all reservations when type filter is set to all', () => {
    const component = createComponent();
    component.reservations.set([
      reservationFixture({ id: 1, estPublic: true }),
      reservationFixture({ id: 2, estPublic: false }),
    ]);

    component.typeFilter = '';

    expect(component.filteredReservations().map((reservation) => reservation.id)).toEqual([1, 2]);
  });

  it('filters public reservations by match type', () => {
    const component = createComponent();
    component.reservations.set([
      reservationFixture({ id: 1, estPublic: true }),
      reservationFixture({ id: 2, estPublic: false }),
    ]);

    component.typeFilter = 'PUBLIC';

    expect(component.filteredReservations().map((reservation) => reservation.id)).toEqual([1]);
  });

  it('filters private reservations by match type', () => {
    const component = createComponent();
    component.reservations.set([
      reservationFixture({ id: 1, estPublic: true }),
      reservationFixture({ id: 2, estPublic: false }),
    ]);

    component.typeFilter = 'PRIVE';

    expect(component.filteredReservations().map((reservation) => reservation.id)).toEqual([2]);
  });

  it('combines the type filter with existing reservation filters', () => {
    const component = createComponent();
    component.reservations.set([
      reservationFixture({ id: 1, site: 'Bruxelles', estPublic: true, estPayee: true }),
      reservationFixture({ id: 2, site: 'Bruxelles', estPublic: false, estPayee: true }),
      reservationFixture({ id: 3, site: 'Namur', estPublic: false, estPayee: false }),
    ]);

    component.searchTerm = 'bruxelles';
    component.paiementFilter = 'PAYEE';
    component.typeFilter = 'PRIVE';

    expect(component.filteredReservations().map((reservation) => reservation.id)).toEqual([2]);
  });

  it('resets reservation filters', () => {
    const component = createComponent();
    component.searchTerm = 'bruxelles';
    component.paiementFilter = 'PAYEE';
    component.statutFilter = 'À venir';
    component.typeFilter = 'PRIVE';

    component.resetFilters();

    expect(component.searchTerm).toBe('');
    expect(component.paiementFilter).toBe('');
    expect(component.statutFilter).toBe('');
    expect(component.typeFilter).toBe('');
  });

  it('does not pay when confirmation is cancelled', () => {
    const dialog = dialogMock(false);
    const component = createComponent({ dialog });
    const payer = vi.spyOn(component, 'payerReservation');

    component.confirmerPaiement(reservationFixture({ id: 3 }));

    expect(payer).not.toHaveBeenCalled();
  });

  it('pays after confirmation', () => {
    const dialog = dialogMock(true);
    const component = createComponent({ dialog });
    const payer = vi.spyOn(component, 'payerReservation');

    component.confirmerPaiement(reservationFixture({ id: 3 }));

    expect(payer).toHaveBeenCalledWith(3);
  });
});

function createComponent(overrides: {
  reservationService?: any;
  authService?: any;
  notificationService?: any;
  dialog?: any;
} = {}) {
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

  return new MesReservations(
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

function reservationFixture(overrides: any = {}): Reservation {
  return {
    id: overrides.id ?? 1,
    estPayee: overrides.estPayee ?? false,
    statut: overrides.statut ?? 'CONFIRMEE',
    match: {
      id: overrides.matchId ?? 10,
      site: overrides.site ?? 'Bruxelles',
      terrain: overrides.terrain ?? 'Central',
      dateHeureDebut: overrides.dateHeureDebut ?? futureDate(),
      statut: overrides.matchStatut ?? 'PLANIFIE',
      estPublic: overrides.estPublic ?? true,
      nbParticipants: overrides.nbParticipants ?? 2,
      organisateurMatricule: overrides.organisateurMatricule ?? 'G0002',
    },
  };
}
