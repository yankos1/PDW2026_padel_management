import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { AuthService } from '../../services/auth.service';
import { Membre, TypeMembre } from '../../models/membre';
import { finalize } from 'rxjs';
import { NotificationService } from '../../services/notification.service';
import { getApiErrorMessage } from '../../utils/api-error.util';

@Component({
  selector: 'app-mon-compte',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCard, MatCardContent, MatCardHeader, MatCardTitle],
  templateUrl: './mon-compte.html',
  styleUrl: './mon-compte.css',
})
export class MonCompte {
  user: Membre | null;
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordLoading = false;
  passwordError = '';

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
  ) {
    this.user = this.authService.getUser();
  }

  nomComplet(): string {
    const prenom = this.user?.prenom?.trim();
    const nom = this.user?.nom?.trim();

    return [prenom, nom].filter(Boolean).join(' ') || 'Non renseigne';
  }

  typeMembre(): TypeMembre | null {
    return this.authService.getTypeMembre();
  }

  libelleTypeMembre(): string {
    if (this.authService.isAdmin()) {
      return 'Admin';
    }

    const type = this.typeMembre();

    if (type === 'GLOBAL') {
      return 'Membre global';
    }

    if (type === 'SITE') {
      return this.user?.site?.name ? `Membre du site ${this.user.site.name}` : 'Membre du site';
    }

    if (type === 'LIBRE') {
      return 'Membre libre';
    }

    return 'Non renseigne';
  }

  delaiReservation(): string {
    const delai = this.authService.getDelaiReservation();

    if (delai === null) {
      return 'Non disponible';
    }

    return `${delai} jours avant le match`;
  }

  matricule(): string {
    return this.authService.getMatricule() || 'Non renseigne';
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  changePassword(): void {
    if (this.passwordLoading) {
      return;
    }

    this.passwordError = '';
    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.passwordError = 'Tous les champs sont obligatoires.';
    } else if (this.newPassword.length < 12) {
      this.passwordError = 'Le nouveau mot de passe doit contenir au moins 12 caractères.';
    } else if (this.newPassword !== this.confirmPassword) {
      this.passwordError = 'Les mots de passe ne correspondent pas.';
    }

    if (this.passwordError) {
      this.notificationService.warning(this.passwordError);
      return;
    }

    this.passwordLoading = true;
    this.authService.changeAdminPassword(this.currentPassword, this.newPassword, this.confirmPassword)
      .pipe(finalize(() => (this.passwordLoading = false)))
      .subscribe({
        next: () => {
          this.currentPassword = '';
          this.newPassword = '';
          this.confirmPassword = '';
          this.notificationService.success('Mot de passe modifié avec succès.');
        },
        error: (error) => {
          this.passwordError = getApiErrorMessage(error, 'Modification du mot de passe impossible');
          this.notificationService.error(this.passwordError);
        },
      });
  }

  penaliteActive(): boolean {
    if (!this.user?.penaliteActive || !this.user.finPenalite) {
      return false;
    }

    return new Date(this.user.finPenalite).getTime() > Date.now();
  }

  libellePenalite(): string {
    if (!this.penaliteActive()) {
      return 'Aucune';
    }

    const finPenalite = this.user?.finPenalite;

    return finPenalite
      ? `Active jusqu'au ${this.formatDateHeure(finPenalite)}`
      : 'Active';
  }

  private formatDateHeure(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-BE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }
}
