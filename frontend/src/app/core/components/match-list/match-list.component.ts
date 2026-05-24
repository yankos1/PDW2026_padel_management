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
  matchs = signal<any[]>([]);

  constructor(
    private matchService: MatchService,
    private reservationService: ReservationService,
    private authService: AuthService,
  ) {}

  ngOnInit() {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      console.error('Pas de matricule');
      return;
    }

    this.matchService.getMatchDisponibles().subscribe((matchs) => {
      this.reservationService.getMesReservations(matricule).subscribe((reservations) => {
        const matchReserveId = reservations.map((r: any) => r.match.id);
        const matchFiltre = matchs.filter(
          (m: any) => !matchReserveId.includes(m.id) && !this.matchDejaPasse(m),
        );

        this.matchs.set(matchFiltre);
      });
    });
  }

  rejoindreMatch(match: any) {
    const matricule = this.authService.getMatricule();

    console.log('Match sélectionné:', match);
    console.log(matricule);

    if (!matricule) {
      console.error('Pas de matricule !');
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
        next: (res) => {
          console.log('réservation réussir', res);

          this.ngOnInit();
          alert('Tu as rejoint le match ');
        },
        error: (err) => {
          console.log(err);
          const message = err.error;
          alert(message);
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
        this.reservationService.payerReservation(reservation.id).subscribe();
        console.log('Paiement réussi');
        this.ngOnInit();
      },
      error: (err) => {
        alert(err.error);
      },
    });
  }

  ouvrirPaiement(match: any) {
    if (!this.peutReserver(match)) {
      alert(this.raisonBlocageReservation(match));
      return;
    }

    const confirm = window.confirm('Paiement de 15€ requis. Continuer ?');

    if (confirm) {
      this.payerMatchPublic(match);
    }
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

  private matchDejaPasse(match: any): boolean {
    return new Date(match.dateHeureDebut).getTime() <= Date.now();
  }
}
