export type StatutMatch = 'PLANIFIE' | 'TERMINE' | 'ANNULE' | 'COMPLET';

export interface Match {
  id: number;
  nbParticipants: number;
  terrainNom?: string;
  terrain?: string;
  siteId?: number;
  site?: string;
  dateHeureDebut: string;
  organisateurMatricule: string;
  estPublic: boolean;
  statut: StatutMatch;
}

export interface CreateMatchPayload {
  organisateur_matricule?: string;
  terrainID: number;
  date: string;
  estPublic: boolean;
}
