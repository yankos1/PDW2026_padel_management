import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private api = "http://localhost:8080/admin";

  constructor(private http: HttpClient) {}

  getMatchs(matricule: string){
    return this.http.get<number>(`${this.api}/matchs?matricule=${matricule}`);
  }

  getCA(matricule: string){
    return this.http.get<number>(`${this.api}/ca?matricule=${matricule}`);
  }

  getMembres(matricule: string){
    return this.http.get<number>(`${this.api}/membres?matricule=${matricule}`);
  }

  getTerrains(matricule: string){
    return this.http.get<number>(`${this.api}/terrains?matricule=${matricule}`);
  }

  getTauxRemplissage(matricule: string){
    return this.http.get<number>(`${this.api}/taux-remplissage?matricule=${matricule}`);
  }

  getRevenusParSite(matricule: string){
    return this.http.get<any>(`${this.api}/revenus-par-site?matricule=${matricule}`);
  }
}
