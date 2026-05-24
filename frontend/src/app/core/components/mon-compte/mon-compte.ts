import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { AuthService } from '../../services/auth.service';
import { Membre, TypeMembre } from '../../models/membre';

@Component({
  selector: 'app-mon-compte',
  standalone: true,
  imports: [CommonModule, MatCard, MatCardContent, MatCardHeader, MatCardTitle],
  templateUrl: './mon-compte.html',
  styleUrl: './mon-compte.css',
})
export class MonCompte {
  user: Membre | null;

  constructor(private authService: AuthService) {
    this.user = this.authService.getUser();
  }

  nomComplet(): string {
    const prenom = this.user?.prenom?.trim();
    const nom = this.user?.nom?.trim();

    return [prenom, nom].filter(Boolean).join(' ') || 'Non renseigné';
  }

  typeMembre(): TypeMembre | null {
    return this.authService.getTypeMembre();
  }

  libelleTypeMembre(): string {
    const type = this.typeMembre();

    if (type === 'GLOBAL') {
      return 'Membre global';
    }

    if (type === 'SITE') {
      return 'Membre du site';
    }

    if (type === 'LIBRE') {
      return 'Membre libre';
    }

    return 'Non renseigné';
  }

  delaiReservation(): string {
    const delai = this.authService.getDelaiReservation();

    if (delai === null) {
      return 'Non disponible';
    }

    return `${delai} jours avant le match`;
  }

  matricule(): string {
    return this.authService.getMatricule() || 'Non renseigné';
  }
}
