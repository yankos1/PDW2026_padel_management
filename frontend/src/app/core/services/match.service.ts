import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class MatchService{
  private api = 'http://localhost:8080/match';

  constructor(private http:HttpClient) {}

  getMatchDisponibles(){
    return this.http.get<any[]>(`${this.api}/disponibles`)
  }
}
