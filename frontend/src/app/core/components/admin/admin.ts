import { Component, OnInit } from '@angular/core';
import {AdminService} from '../../services/admin.service';
import {AuthService} from '../../services/auth.service';
import {DecimalPipe, KeyValuePipe} from '@angular/common';
import {MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle} from '@angular/material/card';

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

    this.adminService.getMatchs(matricule).subscribe({
      next: (res) => (this.matchs = res),
      error: () => (this.error = 'Erreur chargement matchs'),
    });

    this.adminService.getCA(matricule).subscribe({
      next: (res) => (this.ca = res),
      error: () => (this.error = 'Erreur CA'),
    });

    this.adminService.getMembres(matricule).subscribe({
      next: (res) => (this.membres = res),
      error: () => (this.error = 'Erreur membres'),
    });

    this.adminService.getTerrains(matricule).subscribe({
      next: (res) => (this.terrains = res),
      error: () => (this.error = 'Erreur terrains'),
    });

    this.adminService.getTauxRemplissage(matricule).subscribe({
      next: (res) => (this.taux = res),
      error: () => (this.error = 'Erreur taux'),
    });

    this.adminService.getRevenusParSite(matricule).subscribe({
      next: (res) => {
        this.revenus = res;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur revenus';
        this.loading = false;
      },
    });
  }
}
