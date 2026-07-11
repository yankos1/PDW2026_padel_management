import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AdminDashboard, DashboardFilters } from '../models/admin-dashboard';
import { Site } from '../models/site';
import { Terrain } from '../models/terrain';

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

  getTerrainsAccessibles(siteId?: number) {
    let params = new HttpParams();
    if (siteId !== undefined) {
      params = params.set('siteId', siteId);
    }

    return this.http.get<Terrain[]>(`${this.api}/terrains-accessibles`, { params });
  }

  getDashboard(filters: DashboardFilters) {
    let params = new HttpParams()
      .set('dateDebut', filters.dateDebut)
      .set('dateFin', filters.dateFin);

    if (filters.siteId !== undefined) {
      params = params.set('siteId', filters.siteId);
    }

    if (filters.terrainId !== undefined) {
      params = params.set('terrainId', filters.terrainId);
    }

    return this.http.get<AdminDashboard>(`${this.api}/dashboard`, { params });
  }
}
