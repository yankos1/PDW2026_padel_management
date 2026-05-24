export type StatutMatch = 'PLANIFIE'| 'TERMINE'| 'ANNULE'| 'COMPLET'

export interface Match {
  id: number;
  terrainNom: string;
  terrain?: string;
  siteId?: number;
  site?: string;
  dateHeureDebut: string;
  organisateurMatricule: string;
  estPublic: boolean;
  statut: StatutMatch;
}
