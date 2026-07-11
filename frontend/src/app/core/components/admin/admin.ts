import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { finalize, Observable } from 'rxjs';
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
import { MatButton } from '@angular/material/button';

type StatKey = 'matchs' | 'ca' | 'membres' | 'terrains' | 'taux';

@Component({
  selector: 'app-admin',
  imports: [
    DecimalPipe,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatButton,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin implements OnInit {
  matchs: number | null = null;
  ca: number | null = null;
  membres: number | null = null;
  terrains: number | null = null;
  taux: number | null = null;
  revenus: Record<string, number> | null = null;
  sites: Site[] = [];

  loadingStats: Record<StatKey, boolean> = {
    matchs: false,
    ca: false,
    membres: false,
    terrains: false,
    taux: false,
  };
  statErrors: Record<StatKey, string> = {
    matchs: '',
    ca: '',
    membres: '',
    terrains: '',
    taux: '',
  };
  loadingSites = false;
  loadingRevenus = false;
  sitesError = '';
  revenusError = '';
  error = '';

  constructor(
    private adminService: AdminService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.loadDashboard();
  }

  loadDashboard() {
    if (!this.authService.isAdmin()) {
      this.error = 'Accès refusé';
      return;
    }

    if (!this.authService.getMatricule()) {
      this.error = 'Utilisateur non connecté';
      return;
    }

    this.error = '';
    this.loadStat('matchs', this.adminService.getMatchs(), 'Impossible de charger le nombre de matchs.');
    this.loadStat('ca', this.adminService.getCA(), 'Impossible de charger le chiffre d’affaires.');
    this.loadStat('membres', this.adminService.getMembres(), 'Impossible de charger le nombre de membres.');
    this.loadStat('terrains', this.adminService.getTerrains(), 'Impossible de charger le nombre de terrains.');
    this.loadStat('taux', this.adminService.getTauxRemplissage(), 'Impossible de charger le taux de remplissage.');
    this.loadSites();
    this.loadRevenus();
  }

  loadStat(key: StatKey, request$: Observable<number>, fallback: string) {
    this.statErrors[key] = '';
    this.loadingStats[key] = true;

    request$.pipe(finalize(() => {
      this.loadingStats[key] = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: (value) => {
        this[key] = value;
      },
      error: (err) => {
        this.statErrors[key] = getApiErrorMessage(err, fallback);
      },
    });
  }

  loadSites() {
    this.sitesError = '';
    this.loadingSites = true;

    this.adminService.getSites()
      .pipe(finalize(() => {
        this.loadingSites = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (sites) => {
          this.sites = sites;
        },
        error: (err) => {
          this.sitesError = getApiErrorMessage(err, 'Impossible de charger les sites.');
        },
      });
  }

  loadRevenus() {
    this.revenusError = '';
    this.loadingRevenus = true;

    this.adminService.getRevenusParSite()
      .pipe(finalize(() => {
        this.loadingRevenus = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (revenus) => {
          this.revenus = revenus;
        },
        error: (err) => {
          this.revenusError = getApiErrorMessage(err, 'Impossible de charger les revenus par site.');
        },
      });
  }

  retryStat(key: StatKey) {
    switch (key) {
      case 'matchs':
        this.loadStat('matchs', this.adminService.getMatchs(), 'Impossible de charger le nombre de matchs.');
        break;
      case 'ca':
        this.loadStat('ca', this.adminService.getCA(), 'Impossible de charger le chiffre d’affaires.');
        break;
      case 'membres':
        this.loadStat('membres', this.adminService.getMembres(), 'Impossible de charger le nombre de membres.');
        break;
      case 'terrains':
        this.loadStat('terrains', this.adminService.getTerrains(), 'Impossible de charger le nombre de terrains.');
        break;
      case 'taux':
        this.loadStat('taux', this.adminService.getTauxRemplissage(), 'Impossible de charger le taux de remplissage.');
        break;
    }
  }
}
