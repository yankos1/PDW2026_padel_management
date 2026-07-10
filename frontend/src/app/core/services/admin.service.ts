import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Site } from '../models/site';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private api = '/api/admin';

  constructor(private http: HttpClient) {}

  getMatchs() {
    return this.http.get<number>(`${this.api}/matchs`);
  }

  getCA() {
    return this.http.get<number>(`${this.api}/ca`);
  }

  getMembres() {
    return this.http.get<number>(`${this.api}/membres`);
  }

  getTerrains() {
    return this.http.get<number>(`${this.api}/terrains`);
  }

  getTauxRemplissage() {
    return this.http.get<number>(`${this.api}/taux-remplissage`);
  }

  getRevenusParSite() {
    return this.http.get<Record<string, number>>(`${this.api}/revenus-par-site`);
  }

  getSites() {
    return this.http.get<Site[]>(`${this.api}/sites`);
  }
}
