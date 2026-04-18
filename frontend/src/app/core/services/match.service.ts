import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Terrain } from '../models/terrain';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private api = 'http://localhost:8080/match';

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
}
