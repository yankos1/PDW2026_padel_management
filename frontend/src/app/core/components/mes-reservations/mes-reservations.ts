import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
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
import { MatOption, MatSelect } from '@angular/material/select';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { ReservationService } from '../../services/reservation.service';
import { getApiErrorMessage } from '../../utils/api-error.util';
import { ConfirmationDialogComponent } from '../confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-mes-reservations',
  imports: [
    CommonModule,
    MatCard,
    MatCardTitle,
    MatCardContent,
    MatCardActions,
    MatCardHeader,
    MatCardSubtitle,
    MatButton,
    FormsModule,
    MatFormField,
    MatInput,
    MatLabel,
    MatSelect,
    MatOption,
  ],
  templateUrl: './mes-reservations.html',
  styleUrl: './mes-reservations.css',
})
export class MesReservations {
  reservations = signal<any[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  payingReservationId = signal<number | null>(null);
  addingPlayerMatchId = signal<number | null>(null);
  joueurMatricules: Record<number, string> = {};
  erreursAjout: Record<number, string> = {};
  searchTerm = '';
  paiementFilter = '';
  statutFilter = '';

  constructor(
    private reservationService: ReservationService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.loadReservations();
  }

  loadReservations() {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      const message = 'Vous devez être connecté pour consulter vos réservations.';
      this.error.set(message);
      this.notificationService.warning(message);
      return;
    }

    this.error.set(null);
    this.loading.set(true);

    this.reservationService.getMesReservations(matricule)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => {
          this.reservations.set(data);
        },
        error: (err) => {
          const message = getApiErrorMessage(err, 'Impossible de charger vos réservations.');
          this.error.set(message);
          this.notificationService.error(message);
        },
      });
  }

  confirmerPaiement(reservation: any) {
    this.dialog.open(ConfirmationDialogComponent, {
      data: {
        title: 'Confirmer le paiement',
        message: `Confirmer le paiement de ${this.formatMontant(15)} pour cette réservation ?`,
      },
      width: '420px',
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.payerReservation(reservation.id);
      }
    });
  }

  payerReservation(id: number) {
    if (this.payingReservationId() !== null) {
      return;
    }

    this.error.set(null);
    this.payingReservationId.set(id);

    this.reservationService.payerReservation(id)
      .pipe(finalize(() => this.payingReservationId.set(null)))
      .subscribe({
        next: () => {
          this.notificationService.success('Paiement enregistré avec succès.');
          this.loadReservations();
        },
        error: (err) => {
          this.notificationService.error(getApiErrorMessage(err, 'Erreur lors du paiement.'));
        },
      });
  }

  ajouterJoueurMatchPrive(reservation: any) {
    const organisateurMatricule = this.authService.getMatricule();
    const joueurMatricule = this.joueurMatricules[reservation.match.id]?.trim().toUpperCase();

    if (!organisateurMatricule || !joueurMatricule) {
      this.erreursAjout[reservation.match.id] = 'Matricule obligatoire';
      return;
    }

    if (this.addingPlayerMatchId() !== null) {
      return;
    }

    this.erreursAjout[reservation.match.id] = '';
    this.addingPlayerMatchId.set(reservation.match.id);

    this.reservationService.ajouterJoueurMatchPrive({
      organisateurMatricule,
      joueurMatricule,
      matchId: reservation.match.id,
    }).pipe(finalize(() => this.addingPlayerMatchId.set(null))).subscribe({
      next: () => {
        this.joueurMatricules[reservation.match.id] = '';
        this.erreursAjout[reservation.match.id] = '';
        this.notificationService.success('Le joueur a été ajouté au match privé.');
        this.loadReservations();
      },
      error: (err) => {
        const message = getApiErrorMessage(err, 'Erreur lors de l’ajout du joueur.');
        this.erreursAjout[reservation.match.id] = message;
        this.notificationService.error(message);
      },
    });
  }

  reservationsAVenir(): any[] {
    return this.filteredReservations().filter((reservation: any) => !this.reservationPasseeOuAnnulee(reservation));
  }

  reservationsPasseesOuAnnulees(): any[] {
    return this.filteredReservations().filter((reservation: any) => this.reservationPasseeOuAnnulee(reservation));
  }

  filteredReservations(): any[] {
    const search = this.normalize(this.searchTerm);

    return this.reservations().filter((reservation) => {
      const paiementMatches = !this.paiementFilter
        || (this.paiementFilter === 'PAYEE' && this.estPayee(reservation))
        || (this.paiementFilter === 'NON_PAYEE' && !this.estPayee(reservation));
      const statutMatches = !this.statutFilter || this.libelleStatut(reservation) === this.statutFilter;

      if (!paiementMatches || !statutMatches) {
        return false;
      }

      return !search || this.searchableReservationText(reservation).includes(search);
    });
  }

  hasActiveFilters(): boolean {
    return Boolean(this.searchTerm.trim() || this.paiementFilter || this.statutFilter);
  }

  resetFilters() {
    this.searchTerm = '';
    this.paiementFilter = '';
    this.statutFilter = '';
  }

  reservationPasseeOuAnnulee(reservation: any): boolean {
    const statutMatch = reservation.match?.statut;

    return (
      statutMatch === 'TERMINE' ||
      statutMatch === 'ANNULE' ||
      reservation.statut === 'ANNULEE' ||
      new Date(reservation.match?.dateHeureDebut).getTime() <= Date.now()
    );
  }

  libelleStatut(reservation: any): string {
    if (reservation.match?.statut === 'ANNULE' || reservation.statut === 'ANNULEE') {
      return 'Annulé';
    }

    if (reservation.match?.statut === 'TERMINE' || this.reservationPasseeOuAnnulee(reservation)) {
      return 'Terminé';
    }

    if (reservation.statut === 'EN_ATTENTE') {
      return 'En attente';
    }

    return 'À venir';
  }

  statutClass(reservation: any): string {
    const statut = this.libelleStatut(reservation);
    if (statut === 'Annulé') return 'error';
    if (statut === 'Terminé') return 'success';
    if (statut === 'En attente') return 'warning';
    return 'info';
  }

  estPayee(reservation: any): boolean {
    return reservation.paye === true || reservation.estPayee === true;
  }

  paiementClass(reservation: any): string {
    return this.estPayee(reservation) ? 'success' : 'warning';
  }

  libelleTypeMatch(reservation: any): string {
    return reservation.match?.estPublic ? 'Match public' : 'Match privé';
  }

  estResponsable(reservation: any): boolean {
    return this.authService.getMatricule() === reservation.match?.organisateurMatricule;
  }

  placesRestantes(reservation: any): number {
    return Math.max(0, 4 - (reservation.match?.nbParticipants ?? 0));
  }

  peutAjouterJoueurPrive(reservation: any): boolean {
    return (
      this.estResponsable(reservation) &&
      reservation.match?.estPublic === false &&
      reservation.match?.nbParticipants < 4 &&
      !this.reservationPasseeOuAnnulee(reservation)
    );
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

  private searchableReservationText(reservation: any): string {
    return this.normalize([
      reservation.match?.site,
      reservation.match?.terrain,
      reservation.match?.terrainNom,
      this.formatDate(reservation.match?.dateHeureDebut),
      this.formatTime(reservation.match?.dateHeureDebut),
      this.libelleStatut(reservation),
      this.estPayee(reservation) ? 'Payé' : 'Non payé',
    ].filter(Boolean).join(' '));
  }

  private normalize(value: string): string {
    return value.trim().toLocaleLowerCase('fr-FR');
  }
}
