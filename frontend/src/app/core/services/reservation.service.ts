import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  private api = '/api/reservation';


  constructor(private http: HttpClient) {}

  rejoindreMatch(input: { matricule: any; matchId: any }) {
    return this.http.post(`${this.api}/rejoindre`, input);
  }

  ajouterJoueurMatchPrive(input: {
    organisateurMatricule: string;
    joueurMatricule: string;
    matchId: number;
  }) {
    return this.http.post(`${this.api}/match-prive/ajouter-joueur`, input);
  }

  getMesReservations(matricule: string) {
    return this.http.get<any[]>(`${this.api}/membre/${matricule}`);
  }

  payerReservation(id: number) {
    return this.http.put(`${this.api}/${id}/payer`, {});
  }
}
