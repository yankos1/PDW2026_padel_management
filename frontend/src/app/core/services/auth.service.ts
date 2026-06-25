import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Membre } from '../models/membre';
import { TypeMembre } from '../models/membre';

export interface AdminPasswordStatus {
  admin: boolean;
  passwordCreation: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = '/api/auth';


  constructor(private http: HttpClient) {}

  login(matricule: string, password?: string) {
    return this.http.post<Membre>(`${this.api}/login`, {
      matricule: matricule,
      password: password,
    });
  }

  getAdminPasswordStatus(matricule: string) {
    return this.http.get<AdminPasswordStatus>(
      `${this.api}/admin-password-status/${encodeURIComponent(matricule)}`,
    );
  }

  register(input: {
    nom: string;
    prenom: string;
    email: string;
  }) {
    return this.http.post<Membre>(`${this.api}/register`, input);
  }

  setUser(user: Membre) {
    // TODO [IMPORTANT][SECURITE] Remplacer localStorage par une gestion de token JWT validee par le backend.
    localStorage.setItem('user', JSON.stringify(user));
  }

  getUser(): Membre | null {
    return JSON.parse(localStorage.getItem('user') || 'null') as Membre | null;
  }

  getMatricule(): string | null {
    const user = this.getUser();
    return user?.matricule?.trim().toUpperCase() ?? null;
  }

  getTypeMembre(): TypeMembre | null {
    const user = this.getUser();
    const typeMembre = user?.typeMembre?.toUpperCase();

    if (typeMembre === 'GLOBAL' || typeMembre === 'SITE' || typeMembre === 'LIBRE') {
      return typeMembre;
    }

    const matricule = this.getMatricule();

    if (!matricule) {
      return null;
    }

    if (matricule.startsWith('G')) {
      return 'GLOBAL';
    }

    if (matricule.startsWith('S')) {
      return 'SITE';
    }

    if (matricule.startsWith('L')) {
      return 'LIBRE';
    }

    return null;
  }

  getDelaiReservation(): number | null {
    const typeMembre = this.getTypeMembre();

    if (typeMembre === 'GLOBAL') {
      return 21;
    }

    if (typeMembre === 'SITE') {
      return 14;
    }

    if (typeMembre === 'LIBRE') {
      return 5;
    }

    return null;
  }

  getSiteMembreId(): number | null {
    const user = this.getUser();
    const siteId = user?.site?.id;

    if (typeof siteId === 'number') {
      return siteId;
    }

    if (typeof siteId === 'string') {
      return Number(siteId);
    }

    return null;
  }

  canReserveDate(dateMatch: string | Date): boolean {
    const delai = this.getDelaiReservation();

    if (delai === null) {
      return false;
    }

    const maintenant = new Date();
    maintenant.setHours(0, 0, 0, 0);
    const matchDate = typeof dateMatch === 'string' ? new Date(dateMatch) : dateMatch;
    matchDate.setHours(0, 0, 0, 0);
    const dateMax = new Date(maintenant);
    dateMax.setDate(dateMax.getDate() + delai);

    return matchDate >= maintenant && matchDate <= dateMax;
  }

  getDateMaxReservation(): string | null {
    const delai = this.getDelaiReservation();

    if (delai === null) {
      return null;
    }

    const dateMax = new Date();
    dateMax.setDate(dateMax.getDate() + delai);
    return this.formatDateInput(dateMax);
  }

  getDateMinReservation(): string {
    return this.formatDateInput(new Date());
  }

  private formatDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  logout() {
    localStorage.removeItem('user');
    window.location.href = '/login';
  }

  getRole(): string | null {
    const user = this.getUser();
    return user?.role ?? null;
  }

  isAdmin():boolean{
    const role = this.getRole();
    return role == 'ADMIN_GLOBAL' || role == 'ADMIN_SITE';
  }

  isAdminGlobal():boolean{
    return this.getRole() == 'ADMIN_GLOBAL';
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('user');
  }
}
