import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { catchError, combineLatest, of } from 'rxjs';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';

import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';

@Component({
  selector: 'app-admin',
  imports: [
    DecimalPipe,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    KeyValuePipe,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin implements OnInit {
  matchs = 0;
  ca = 0;
  membres = 0;
  terrains = 0;
  taux = 0;
  revenus: Record<string, number> = {};

  loading = true;
  error = '';

  constructor(
    private adminService: AdminService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (!this.authService.isAdmin()) {
      this.error = 'Acces refuse';
      this.loading = false;
      return;
    }

    if (!this.authService.getMatricule()) {
      this.error = 'Utilisateur non connecte';
      this.loading = false;
      return;
    }

    combineLatest([
      this.adminService.getMatchs().pipe(catchError(() => of(0))),
      this.adminService.getCA().pipe(catchError(() => of(0))),
      this.adminService.getMembres().pipe(catchError(() => of(0))),
      this.adminService.getTerrains().pipe(catchError(() => of(0))),
      this.adminService.getTauxRemplissage().pipe(catchError(() => of(0))),
      this.adminService.getRevenusParSite().pipe(catchError(() => of({}))),
    ]).subscribe({
      next: ([matchs, ca, membres, terrains, taux, revenus]) => {
        this.matchs = matchs;
        this.ca = ca;
        this.membres = membres;
        this.terrains = terrains;
        this.taux = taux;
        this.revenus = revenus;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur globale';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
