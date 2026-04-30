import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { DecimalPipe, KeyValuePipe } from '@angular/common';

import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import { catchError, combineLatest, of } from 'rxjs';

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
  revenus: any = {};

  loading = true;
  error = '';

  constructor(
    private adminService: AdminService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    if (!this.authService.isAdmin()) {
      this.error = 'Accès refusé';
      this.loading = false;
      return;
    }

    const matricule = this.authService.getMatricule();

    if (!matricule) {
      this.error = 'Utilisateur non connecté';
      this.loading = false;
      return;
    }

    combineLatest([
      this.adminService.getMatchs(matricule).pipe(catchError(() => of(0))),
      this.adminService.getCA(matricule).pipe(catchError(() => of(0))),
      this.adminService.getMembres(matricule).pipe(catchError(() => of(0))),
      this.adminService.getTerrains(matricule).pipe(catchError(() => of(0))),
      this.adminService.getTauxRemplissage(matricule).pipe(catchError(() => of(0))),
      this.adminService.getRevenusParSite(matricule).pipe(catchError(() => of({}))),
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
