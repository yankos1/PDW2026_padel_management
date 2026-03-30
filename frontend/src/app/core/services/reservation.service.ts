import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class ReservationService {
  private api = 'http://localhost:8080/reservation';

  constructor(private http: HttpClient) {}

  rejoindreMatch(input: { matricule: any; matchId: any }) {
    return this.http.post(`${this.api}/rejoindre`, input);
  }

  getMesReservations(matricule: string) {
    return this.http.get<any[]>(`${this.api}/membre/${matricule}`);
  }

  payerReservation(id: number) {
    return this.http.put(`${this.api}/${id}/payer`, {});
  }
}
