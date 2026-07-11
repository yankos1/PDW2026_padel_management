export interface DashboardSummary {
  chiffreAffaires: number;
  nombreMatchs: number;
  reservationsConfirmees: number;
  tauxRemplissageMatchs: number;
  tauxOccupationTerrains: number;
  soldesDus: number;
  membresActifs: number;
  matchsAnnules: number;
  matchsProchainsIncomplets: number;
}

export interface RevenuePeriod {
  annee: number;
  mois: number;
  montant: number;
}

export interface MatchPeriod {
  annee: number;
  mois: number;
  nombre: number;
}

export interface TerrainStatistics {
  terrainId: number;
  terrainNom: string;
  siteId: number;
  siteNom: string;
  nombreMatchs: number;
  reservationsValides: number;
  tauxRemplissage: number;
}

export interface MatchStatusStatistics {
  statut: string;
  nombre: number;
}

export interface IncompleteMatch {
  matchId: number;
  dateHeureDebut: string;
  terrainId: number;
  terrainNom: string;
  siteId: number;
  siteNom: string;
  participants: number;
  placesRestantes: number;
}

export interface AdminDashboard {
  resume: DashboardSummary;
  chiffreAffairesParMois: RevenuePeriod[];
  matchsParMois: MatchPeriod[];
  tauxRemplissageParTerrain: TerrainStatistics[];
  repartitionMatchsParStatut: MatchStatusStatistics[];
  terrainsLesPlusUtilises: TerrainStatistics[];
  prochainsMatchsIncomplets: IncompleteMatch[];
}

export interface DashboardFilters {
  dateDebut: string;
  dateFin: string;
  siteId?: number;
  terrainId?: number;
}
