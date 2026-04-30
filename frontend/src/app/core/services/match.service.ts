import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Terrain } from '../models/terrain';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private api = '/api/match';

  constructor(private http: HttpClient) {}

  getMatchDisponibles() {
    return this.http.get<any[]>(`${this.api}/disponibles`);
  }
  createMatch(dto: {
    organisateur_matricule: string;
    terrainID: number;
    date: string;
    estPublic: boolean;
  }) {
    return this.http.post(`${this.api}`, dto);
  }
  getTerrains() {
    return this.http.get<Terrain[]>(`${this.api}/terrains`);
  }

  getTerrainsDisponibles(date: string) {
    return this.http.get<Terrain[]>(`${this.api}/terrains/disponibles?date=${date}`);
  }
  getTerrainsDisponiblesParCreneau(date: string, heureSelected: string) {
    return this.http.get<Terrain[]>(
      `${this.api}/terrains/disponibles-par-creneau?date=${date}&heure=${heureSelected}`
    );
  }
  getCreneauxDisponibles(date:string){
    return this.http.get<string[]>(`${this.api}/creneaux-disponibles?date=${date}`);
  }
}
