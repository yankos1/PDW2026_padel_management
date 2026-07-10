import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { MatchListComponent } from './match-list.component';

describe('MatchListComponent', () => {
  it('keeps previous matches and shows an error when reservations fail', () => {
    const matchService = {
      getMatchDisponibles: vi.fn().mockReturnValue(of([])),
    };
    const reservationService = {
      getMesReservations: vi.fn().mockReturnValue(throwError(() => ({
        error: {
          status: 401,
          message: 'Authentification requise',
          timestamp: '2026-07-10T18:00:00Z',
          fieldErrors: {},
        },
      }))),
      rejoindreMatch: vi.fn(),
      payerReservation: vi.fn(),
    };
    const authService = {
      getMatricule: vi.fn().mockReturnValue('G0002'),
      canReserveDate: vi.fn().mockReturnValue(true),
      getDelaiReservation: vi.fn().mockReturnValue(21),
      getSiteMembreId: vi.fn().mockReturnValue(null),
      getTypeMembre: vi.fn().mockReturnValue('GLOBAL'),
    };
    const component = new MatchListComponent(
      matchService as any,
      reservationService as any,
      authService as any,
    );
    component.matchs.set([{ id: 99, dateHeureDebut: new Date(Date.now() + 60_000).toISOString() }]);

    component.ngOnInit();

    expect(component.matchs().map((match: any) => match.id)).toEqual([99]);
    expect(component.error()).toBe('Authentification requise');
    expect(matchService.getMatchDisponibles).toHaveBeenCalledTimes(1);
    expect(reservationService.getMesReservations).toHaveBeenCalledTimes(1);
  });

  it('filters already reserved matches without duplicate calls', () => {
    const matchs = [
      { id: 1, dateHeureDebut: new Date(Date.now() + 60_000).toISOString() },
      { id: 2, dateHeureDebut: new Date(Date.now() + 60_000).toISOString() },
    ];
    const matchService = {
      getMatchDisponibles: vi.fn().mockReturnValue(of(matchs)),
    };
    const reservationService = {
      getMesReservations: vi.fn().mockReturnValue(of([{ match: { id: 1 } }])),
      rejoindreMatch: vi.fn(),
      payerReservation: vi.fn(),
    };
    const authService = {
      getMatricule: vi.fn().mockReturnValue('G0002'),
      canReserveDate: vi.fn().mockReturnValue(true),
      getDelaiReservation: vi.fn().mockReturnValue(21),
      getSiteMembreId: vi.fn().mockReturnValue(null),
      getTypeMembre: vi.fn().mockReturnValue('GLOBAL'),
    };
    const component = new MatchListComponent(
      matchService as any,
      reservationService as any,
      authService as any,
    );

    component.ngOnInit();

    expect(component.matchs().map((match: any) => match.id)).toEqual([2]);
    expect(matchService.getMatchDisponibles).toHaveBeenCalledTimes(1);
    expect(reservationService.getMesReservations).toHaveBeenCalledTimes(1);
  });
});
