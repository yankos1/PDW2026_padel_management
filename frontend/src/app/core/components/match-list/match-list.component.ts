import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatchService } from '../../services/match.service';
import { MatDivider } from '@angular/material/list';
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ReservationService } from '../../services/reservation.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-match-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatCardActions,
    MatDivider,
    MatButtonModule,
  ],
  templateUrl: './match-list.component.html',
  styleUrls: ['./match-list.component.css'],
})
export class MatchListComponent implements OnInit {
  // TODO [IMPORTANT] Remplacer any par des interfaces TypeScript alignees sur les DTO backend.
  matchs = signal<any[]>([]);
  error = signal<string | null>(null);

  constructor(
    private matchService: MatchService,
    private reservationService: ReservationService,
    private authService: AuthService,
  ) {}

  ngOnInit() {
    // TODO [IMPORTANT][UX] Ajouter un etat loading et un etat vide quand aucun match public n'est disponible.
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      console.error('Pas de matricule');
      return;
    }

    this.error.set(null);
    this.matchService.getMatchDisponibles().subscribe({
      next: (matchs) => {
        this.reservationService.getMesReservations(matricule).subscribe({
          next: (reservations) => {
            const matchReserveId = reservations.map((r: any) => r.match.id);
            const matchFiltre = matchs.filter(
              (m: any) => !matchReserveId.includes(m.id) && !this.matchDejaPasse(m),
            );

            this.matchs.set(matchFiltre);
          },
          error: () => this.error.set('Impossible de charger vos reservations.'),
        });
      },
      error: () => {
        this.matchs.set([]);
        this.error.set('Impossible de charger les matchs publics disponibles.');
      },
    });
  }

  rejoindreMatch(match: any) {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      console.error('Pas de matricule');
      return;
    }

    if (!this.peutReserver(match)) {
      alert(this.raisonBlocageReservation(match));
      return;
    }

    this.reservationService
      .rejoindreMatch({
        matricule: matricule,
        matchId: match.id,
      })
      .subscribe({
        next: () => {
          this.ngOnInit();
          alert('Reservation en attente de paiement');
        },
        error: (err) => {
          alert(err.error?.message || err.error || 'Reservation impossible');
        },
      });
  }

  payerMatchPublic(match: any) {
    const matricule = this.authService.getMatricule();
    if (!matricule) {
      alert('Pas de matricule');
      return;
    }

    if (!this.peutReserver(match)) {
      alert(this.raisonBlocageReservation(match));
      return;
    }

    this.reservationService.rejoindreMatch({ matricule: matricule, matchId: match.id }).subscribe({
      next: (reservation: any) => {
        this.reservationService.payerReservation(reservation.id).subscribe({
          next: () => {
            this.ngOnInit();
          },
          error: (err) => {
            alert(err.error?.message || err.error || 'Paiement impossible');
            this.ngOnInit();
          },
        });
      },
      error: (err) => {
        alert(err.error?.message || err.error || 'Reservation impossible');
      },
    });
  }

  ouvrirPaiement(match: any) {
    if (!this.peutReserver(match)) {
      alert(this.raisonBlocageReservation(match));
      return;
    }

    const confirm = window.confirm('Paiement de 15 euros requis pour confirmer la place. Continuer ?');

    if (confirm) {
      this.payerMatchPublic(match);
    }
  }

  peutReserver(match: any): boolean {
    return this.raisonBlocageReservation(match) === null;
  }

  raisonBlocageReservation(match: any): string | null {
    if (this.matchDejaPasse(match)) {
      return 'Ce match est deja passe';
    }

    if (!this.authService.canReserveDate(match.dateHeureDebut)) {
      const delai = this.authService.getDelaiReservation();
      return delai === null
        ? 'Categorie de membre invalide'
        : `Vous pouvez reserver au maximum ${delai} jours avant la date du match`;
    }

    const siteMembreId = this.authService.getSiteMembreId();

    if (
      this.authService.getTypeMembre() === 'SITE' &&
      siteMembreId !== null &&
      match.siteId !== siteMembreId
    ) {
      return 'Un membre du site ne peut reserver que sur son site';
    }

    return null;
  }

  private matchDejaPasse(match: any): boolean {
    return new Date(match.dateHeureDebut).getTime() <= Date.now();
  }
}
