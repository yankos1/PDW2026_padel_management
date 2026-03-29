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

  //TO DO later
  // isLoggedIn(): boolean {
  //   return !!localStorage.getItem('token');
  // }

  getUser() {
    return JSON.parse(localStorage.getItem('user') || 'null');
  }

  logout() {
    localStorage.removeItem('user');
    window.location.href = '/login';
  }
}
