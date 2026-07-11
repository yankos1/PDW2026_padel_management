import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatDivider } from '@angular/material/list';
import { finalize, forkJoin, switchMap } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { MatchService } from '../../services/match.service';
import { NotificationService } from '../../services/notification.service';
import { ReservationService } from '../../services/reservation.service';
import { getApiErrorMessage } from '../../utils/api-error.util';
import { ConfirmationDialogComponent } from '../confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-match-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatCardActions,
    MatDivider,
    MatButtonModule,
    MatFormField,
    MatLabel,
    MatInput,
  ],
  templateUrl: './match-list.component.html',
  styleUrls: ['./match-list.component.css'],
})
export class MatchListComponent implements OnInit {
  // TODO [IMPORTANT] Remplacer any par des interfaces TypeScript alignees sur les DTO backend.
  matchs = signal<any[]>([]);
  error = signal<string | null>(null);
  loading = signal(false);
  joiningMatchId = signal<number | null>(null);
  searchTerm = '';

  constructor(
    private matchService: MatchService,
    private reservationService: ReservationService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.loadMatchs();
  }

  loadMatchs() {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      const message = 'Vous devez être connecté pour consulter les matchs.';
      this.error.set(message);
      this.notificationService.warning(message);
      return;
    }

    this.error.set(null);
    this.loading.set(true);
    forkJoin({
      matchs: this.matchService.getMatchDisponibles(),
      reservations: this.reservationService.getMesReservations(matricule),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ matchs, reservations }) => {
        const matchReserveId = reservations.map((r: any) => r.match.id);
        const matchFiltre = matchs.filter(
          (m: any) => !matchReserveId.includes(m.id) && !this.matchDejaPasse(m),
        );

        this.matchs.set(matchFiltre);
      },
      error: (err) => {
        const message = getApiErrorMessage(err, 'Impossible de charger les matchs publics disponibles.');
        this.error.set(message);
        this.notificationService.error(message);
      },
    });
  }

  rejoindreMatch(match: any) {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      this.notificationService.warning('Vous devez être connecté pour réserver.');
      return;
    }

    if (!this.peutReserver(match)) {
      this.notificationService.warning(this.raisonBlocageReservation(match) ?? 'Réservation impossible.');
      return;
    }

    if (this.joiningMatchId() !== null) {
      return;
    }

    this.error.set(null);
    this.joiningMatchId.set(match.id);

    this.reservationService
      .rejoindreMatch({
        matricule: matricule,
        matchId: match.id,
      })
      .pipe(finalize(() => this.joiningMatchId.set(null)))
      .subscribe({
        next: () => {
          this.notificationService.success('Réservation en attente de paiement.');
          this.loadMatchs();
        },
        error: (err) => {
          this.notificationService.error(getApiErrorMessage(err, 'Réservation impossible.'));
        },
      });
  }

  payerMatchPublic(match: any) {
    const matricule = this.authService.getMatricule();
    if (!matricule) {
      this.notificationService.warning('Vous devez être connecté pour réserver.');
      return;
    }

    if (!this.peutReserver(match)) {
      this.notificationService.warning(this.raisonBlocageReservation(match) ?? 'Paiement impossible.');
      return;
    }

    if (this.joiningMatchId() !== null) {
      return;
    }

    this.error.set(null);
    this.joiningMatchId.set(match.id);

    this.reservationService.rejoindreMatch({ matricule: matricule, matchId: match.id }).pipe(
      switchMap((reservation: any) => this.reservationService.payerReservation(reservation.id)),
      finalize(() => this.joiningMatchId.set(null)),
    ).subscribe({
      next: () => {
        this.notificationService.success('Paiement enregistré avec succès.');
        this.loadMatchs();
      },
      error: (err) => {
        this.notificationService.error(getApiErrorMessage(err, 'Paiement impossible.'));
        this.loadMatchs();
      },
    });
  }

  ouvrirPaiement(match: any) {
    if (!this.peutReserver(match)) {
      this.notificationService.warning(this.raisonBlocageReservation(match) ?? 'Paiement impossible.');
      return;
    }

    this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Confirmer le paiement',
        message: `Confirmer le paiement de ${this.formatMontant(15)} pour cette réservation ?`,
      },
      width: '420px',
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.payerMatchPublic(match);
      }
    });
  }

  peutReserver(match: any): boolean {
    return this.raisonBlocageReservation(match) === null;
  }

  raisonBlocageReservation(match: any): string | null {
    if (this.matchDejaPasse(match)) {
      return 'Ce match est déjà passé';
    }

    if (!this.authService.canReserveDate(match.dateHeureDebut)) {
      const delai = this.authService.getDelaiReservation();
      return delai === null
        ? 'Catégorie de membre invalide'
        : `Vous pouvez réserver au maximum ${delai} jours avant la date du match`;
    }

    const siteMembreId = this.authService.getSiteMembreId();

    if (
      this.authService.getTypeMembre() === 'SITE' &&
      siteMembreId !== null &&
      match.siteId !== siteMembreId
    ) {
      return 'Un membre du site ne peut réserver que sur son site';
    }

    return null;
  }

  filteredMatchs(): any[] {
    const search = this.normalize(this.searchTerm);

    return this.matchs().filter((match) => {
      return !search || this.searchableMatchText(match).includes(search);
    });
  }

  hasActiveFilters(): boolean {
    return Boolean(this.searchTerm.trim());
  }

  resetFilters() {
    this.searchTerm = '';
  }

  libelleStatut(statut: string | null | undefined): string {
    const labels: Record<string, string> = {
      PLANIFIE: 'Planifié',
      TERMINE: 'Terminé',
      ANNULE: 'Annulé',
      COMPLET: 'Complet',
    };

    return statut ? labels[statut] ?? statut : 'Non renseigné';
  }

  statutClass(statut: string | null | undefined): string {
    if (statut === 'PLANIFIE') return 'info';
    if (statut === 'COMPLET') return 'warning';
    if (statut === 'ANNULE') return 'error';
    if (statut === 'TERMINE') return 'success';
    return '';
  }

  libelleTypeMatch(match: any): string {
    return match.estPublic === false ? 'Privé' : 'Public';
  }

  formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value || 'Non renseignée';
    return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
  }

  formatTime(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(date);
  }

  formatMontant(value: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(value);
  }

  private searchableMatchText(match: any): string {
    return this.normalize([
      match.site,
      match.terrain,
      match.terrainNom,
      this.formatDate(match.dateHeureDebut),
      this.formatTime(match.dateHeureDebut),
      match.organisateur,
      match.organisateurMatricule,
      match.matricule,
      this.libelleStatut(match.statut),
      match.statut,
      this.libelleTypeMatch(match),
    ].filter(Boolean).join(' '));
  }

  private normalize(value: string): string {
    return value.trim().toLocaleLowerCase('fr-FR');
  }

  private matchDejaPasse(match: any): boolean {
    return new Date(match.dateHeureDebut).getTime() <= Date.now();
  }
}
