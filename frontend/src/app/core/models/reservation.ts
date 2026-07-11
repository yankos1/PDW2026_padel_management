import { Match } from './match';

export type StatutReservation = 'CONFIRMEE' | 'ANNULEE' | 'EN_ATTENTE';

export interface Reservation {
  id: number;
  matchId?: number;
  match: Match;
  membreMatricule?: string;
  dateReservation?: string;
  paye?: boolean;
  estPayee: boolean;
  statut: StatutReservation;
}

export interface JoinMatchPayload {
  matchId: number;
}

export interface AddPrivateMatchPlayerPayload {
  organisateurMatricule?: string;
  joueurMatricule: string;
  matchId: number;
}
