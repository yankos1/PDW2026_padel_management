import { Component, signal } from '@angular/core';
import { ReservationService } from '../../services/reservation.service';
import { AuthService } from '../../services/auth.service';
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle
} from '@angular/material/card';
import { DatePipe } from '@angular/common';
import { MatButton } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { MatInput } from '@angular/material/input';
import { MatFormField, MatLabel } from '@angular/material/form-field';
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
  reservations = signal<any>([]);
  joueurMatricules: Record<number, string> = {};
  erreursAjout: Record<number, string> = {};

  constructor(
    private reservationService: ReservationService,
    private authService: AuthService,
  ) {}

  ngOnInit() {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      console.error('Utilisateur non connecté');
      return;
    }

    this.reservationService.getMesReservations(matricule).subscribe({
      next: (data) => {
        console.log('Mes réservations:', data);
        this.reservations.set(data);
      },
      error: (err) => console.error(err),
    });
  }

  payerReservation(id: number) {
    this.reservationService.payerReservation(id).subscribe({
      next: () => {
        console.log('Paiement réussi');

        this.ngOnInit();
      },
      error: (err) => {
        console.error('Erreur paiement', err);
        alert(getApiErrorMessage(err, 'Erreur lors du paiement'));
      }
    });
  }

  ajouterJoueurMatchPrive(reservation: any) {
    const organisateurMatricule = this.authService.getMatricule();
    const joueurMatricule = this.joueurMatricules[reservation.match.id]?.trim().toUpperCase();

    if (!organisateurMatricule || !joueurMatricule) {
      this.erreursAjout[reservation.match.id] = 'Matricule obligatoire';
      return;
    }

    this.reservationService.ajouterJoueurMatchPrive({
      organisateurMatricule,
      joueurMatricule,
      matchId: reservation.match.id,
    }).subscribe({
      next: () => {
        this.joueurMatricules[reservation.match.id] = '';
        this.erreursAjout[reservation.match.id] = '';
        this.ngOnInit();
      },
      error: (err) => {
        this.erreursAjout[reservation.match.id] =
          getApiErrorMessage(err, 'Erreur lors de l ajout du joueur');
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
