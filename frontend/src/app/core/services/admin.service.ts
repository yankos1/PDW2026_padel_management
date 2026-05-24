import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private api = '/api/admin';

  constructor(
    private http: HttpClient,
    private authService: AuthService,
  ) {}

  getMatchs() {
    return this.http.get<number>(`${this.api}/matchs`, this.adminOptions());
  }

  getCA() {
    return this.http.get<number>(`${this.api}/ca`, this.adminOptions());
  }

  getMembres() {
    return this.http.get<number>(`${this.api}/membres`, this.adminOptions());
  }

  getTerrains() {
    return this.http.get<number>(`${this.api}/terrains`, this.adminOptions());
  }

  getTauxRemplissage() {
    return this.http.get<number>(`${this.api}/taux-remplissage`, this.adminOptions());
  }

  getRevenusParSite() {
    return this.http.get<Record<string, number>>(`${this.api}/revenus-par-site`, this.adminOptions());
  }

  private adminOptions() {
    const matricule = this.authService.getMatricule() ?? '';
    return {
      headers: new HttpHeaders({
        'X-User-Matricule': matricule,
      }),
    };
  }
}
