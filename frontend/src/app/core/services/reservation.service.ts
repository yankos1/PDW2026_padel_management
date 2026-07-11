import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  AddPrivateMatchPlayerPayload,
  JoinMatchPayload,
  Reservation,
} from '../models/reservation';

@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  private api = '/api/reservation';


  constructor(private http: HttpClient) {}

  rejoindreMatch(input: JoinMatchPayload) {
    return this.http.post<Reservation>(`${this.api}/rejoindre`, input);
  }

  ajouterJoueurMatchPrive(input: AddPrivateMatchPlayerPayload) {
    return this.http.post<Reservation>(`${this.api}/match-prive/ajouter-joueur`, input);
  }

  getMesReservations(matricule: string) {
    return this.http.get<Reservation[]>(`${this.api}/membre/${matricule}`);
  }

  payerReservation(id: number) {
    return this.http.put<Reservation>(`${this.api}/${id}/payer`, {});
  }
}
