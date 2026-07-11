import { Component, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminDashboard, DashboardFilters, IncompleteMatch, MatchPeriod, MatchStatusStatistics, RevenuePeriod, TerrainStatistics } from '../../models/admin-dashboard';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Site } from '../../models/site';
import { Terrain } from '../../models/terrain';
import { getApiErrorMessage } from '../../utils/api-error.util';

import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';

@Component({
  selector: 'app-admin',
  imports: [
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatButton,
    DecimalPipe,
    DatePipe,
    FormsModule,
    MatFormField,
    MatLabel,
    MatInput,
    MatSelect,
    MatOption,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin implements OnInit {
  dashboard: AdminDashboard | null = null;
  sites: Site[] = [];
  terrains: Terrain[] = [];
  filters: DashboardFilters = this.defaultFilters();

  loadingDashboard = false;
  loadingSites = false;
  loadingTerrains = false;
  error = '';

  constructor(
    private adminService: AdminService,
    public authService: AuthService,
    private notificationService: NotificationService,
  ) {}

  ngOnInit() {
    this.loadInitialData();
  }

  loadInitialData() {
    if (!this.authService.isAdmin()) {
      this.error = 'Acces refuse';
      return;
    }

    if (!this.authService.getMatricule()) {
      this.error = 'Utilisateur non connecte';
      return;
    }

    this.error = '';
    this.loadSites();
    this.loadTerrains();
    this.loadDashboard();
  }

  loadSites() {
    if (!this.authService.isAdminGlobal()) {
      return;
    }

    this.loadingSites = true;
    this.adminService.getSites()
      .pipe(finalize(() => this.loadingSites = false))
      .subscribe({
        next: (sites) => this.sites = sites,
        error: (err) => {
          const message = getApiErrorMessage(err, 'Impossible de charger les sites.');
          this.notificationService.error(message);
        },
      });
  }

  loadTerrains() {
    this.loadingTerrains = true;
    this.adminService.getTerrainsAccessibles(this.filters.siteId)
      .pipe(finalize(() => this.loadingTerrains = false))
      .subscribe({
        next: (terrains) => this.terrains = terrains,
        error: (err) => {
          const message = getApiErrorMessage(err, 'Impossible de charger les terrains.');
          this.notificationService.error(message);
        },
      });
  }

  loadDashboard() {
    this.loadingDashboard = true;
    this.error = '';
    this.adminService.getDashboard(this.compactFilters())
      .pipe(finalize(() => this.loadingDashboard = false))
      .subscribe({
        next: (dashboard) => this.dashboard = dashboard,
        error: (err) => {
          const message = getApiErrorMessage(err, 'Impossible de charger le dashboard administrateur.');
          this.error = message;
          this.notificationService.error(message);
        },
      });
  }

  applyFilters() {
    this.loadDashboard();
  }

  onSiteChange() {
    this.filters.terrainId = undefined;
    this.loadTerrains();
    this.loadDashboard();
  }

  resetFilters() {
    this.filters = this.defaultFilters();
    this.loadTerrains();
    this.loadDashboard();
  }

  hasData(): boolean {
    return !!this.dashboard && (
      this.dashboard.resume.nombreMatchs > 0 ||
      this.dashboard.resume.chiffreAffaires > 0 ||
      this.dashboard.resume.soldesDus > 0 ||
      this.dashboard.resume.membresActifs > 0
    );
  }

  maxMonthlyRevenue(): number {
    return this.maxValue(this.dashboard?.chiffreAffairesParMois ?? [], (item) => item.montant);
  }

  maxMonthlyMatches(): number {
    return this.maxValue(this.dashboard?.matchsParMois ?? [], (item) => item.nombre);
  }

  maxTerrainUsage(): number {
    return this.maxValue(this.dashboard?.terrainsLesPlusUtilises ?? [], (item) => item.nombreMatchs);
  }

  barWidth(value: number, max: number): string {
    if (max <= 0) {
      return '0%';
    }

    return `${Math.min(100, Math.round((value / max) * 100))}%`;
  }

  formatMontant(value: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(value);
  }

  formatMonth(period: Pick<RevenuePeriod, 'annee' | 'mois'>): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'short', year: 'numeric' })
      .format(new Date(period.annee, period.mois - 1, 1));
  }

  trackPeriod(_: number, item: RevenuePeriod): string {
    return `${item.annee}-${item.mois}`;
  }

  trackMatchPeriod(_: number, item: MatchPeriod): string {
    return `${item.annee}-${item.mois}`;
  }

  trackTerrain(_: number, item: TerrainStatistics): number {
    return item.terrainId;
  }

  trackStatus(_: number, item: MatchStatusStatistics): string {
    return item.statut;
  }

  trackMatch(_: number, item: IncompleteMatch): number {
    return item.matchId;
  }

  private compactFilters(): DashboardFilters {
    return {
      dateDebut: this.filters.dateDebut,
      dateFin: this.filters.dateFin,
      siteId: this.filters.siteId || undefined,
      terrainId: this.filters.terrainId || undefined,
    };
  }

  private defaultFilters(): DashboardFilters {
    const today = new Date();
    const start = new Date(today.getFullYear(), today.getMonth(), 1);
    return {
      dateDebut: this.formatDateInput(start),
      dateFin: this.formatDateInput(today),
    };
  }

  private formatDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private maxValue<T>(items: T[], selector: (item: T) => number): number {
    return items.reduce((max, item) => Math.max(max, selector(item)), 0);
  }
}
