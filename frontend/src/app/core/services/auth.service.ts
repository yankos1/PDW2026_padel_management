import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Membre } from '../models/membre';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = 'http://localhost:8080/auth';

  constructor(private http: HttpClient) {}

  login(matricule: string) {
    return this.http.post<Membre>(`${this.api}/login`, {
      matricule: matricule,
    });
  }
  getCurrentUser() {
    return JSON.parse(localStorage.getItem('user') || '{}');
  }
}
