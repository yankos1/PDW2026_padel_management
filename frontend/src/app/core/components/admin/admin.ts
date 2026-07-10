import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { combineLatest } from 'rxjs';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { Site } from '../../models/site';
import { getApiErrorMessage } from '../../utils/api-error.util';

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
  sites: Site[] = [];

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

    this.error = '';
    combineLatest([
      this.adminService.getMatchs(),
      this.adminService.getCA(),
      this.adminService.getMembres(),
      this.adminService.getTerrains(),
      this.adminService.getTauxRemplissage(),
      this.adminService.getRevenusParSite(),
      this.adminService.getSites(),
    ]).subscribe({
      next: ([matchs, ca, membres, terrains, taux, revenus, sites]) => {
        this.matchs = matchs;
        this.ca = ca;
        this.membres = membres;
        this.terrains = terrains;
        this.taux = taux;
        this.revenus = revenus;
        this.sites = sites;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.error = getApiErrorMessage(err, 'Impossible de charger le dashboard admin');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
}
