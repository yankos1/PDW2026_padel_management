import { DatePipe } from '@angular/common';
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
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ReservationService } from '../../services/reservation.service';
import { getApiErrorMessage } from '../../utils/api-error.util';

@Component({
  selector: 'app-mes-reservations',
  imports: [
    MatCard,
    MatCardTitle,
    MatCardContent,
    DatePipe,
    MatCardActions,
    MatCardHeader,
    MatCardSubtitle,
    MatButton,
    FormsModule,
    MatFormField,
    MatInput,
    MatLabel,
  ],
  templateUrl: './mes-reservations.html',
  styleUrl: './mes-reservations.css',
})
export class MesReservations {
  // TODO [BONUS] Ajouter recherche, filtres et tri sur les reservations affichees.
  reservations = signal<any[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  payingReservationId = signal<number | null>(null);
  addingPlayerMatchId = signal<number | null>(null);
  joueurMatricules: Record<number, string> = {};
  erreursAjout: Record<number, string> = {};

  constructor(
    private reservationService: ReservationService,
    private authService: AuthService,
  ) {}

  ngOnInit() {
    this.loadReservations();
  }

  loadReservations() {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      this.error.set('Vous devez être connecté pour consulter vos réservations.');
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
          this.error.set(getApiErrorMessage(err, 'Impossible de charger vos réservations.'));
        },
      });
  }

  payerReservation(id: number) {
    if (this.payingReservationId() !== null) {
      return;
    }

    this.error.set(null);
    this.success.set(null);
    this.payingReservationId.set(id);

    this.reservationService.payerReservation(id)
      .pipe(finalize(() => this.payingReservationId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Le paiement a été enregistré.');
          this.loadReservations();
        },
        error: (err) => {
          this.error.set(getApiErrorMessage(err, 'Erreur lors du paiement'));
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

    this.success.set(null);
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
        this.success.set('Le joueur a été ajouté au match privé.');
        this.loadReservations();
      },
      error: (err) => {
        this.erreursAjout[reservation.match.id] =
          getApiErrorMessage(err, 'Erreur lors de l’ajout du joueur');
      },
    });
  }

  reservationsAVenir(): any[] {
    return this.reservations().filter((reservation: any) => !this.reservationPasseeOuAnnulee(reservation));
  }

  reservationsPasseesOuAnnulees(): any[] {
    return this.reservations().filter((reservation: any) => this.reservationPasseeOuAnnulee(reservation));
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

    return 'À venir';
  }

  estPayee(reservation: any): boolean {
    return reservation.paye === true || reservation.estPayee === true;
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
}
