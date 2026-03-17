export type StatutReservation = 'CONFIRMEE'| 'ANNULEE'| 'EN_ATTENTE'

export interface Reservation {
  id: number;
  matchId: number;
  membreMatricule: string;
  dateReservation: string;
  estPayee: boolean;
  statut: StatutReservation;
}
