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
import { finalize, forkJoin, switchMap } from 'rxjs';
import { getApiErrorMessage } from '../../utils/api-error.util';

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
  success = signal<string | null>(null);
  loading = signal(false);
  joiningMatchId = signal<number | null>(null);

  constructor(
    private matchService: MatchService,
    private reservationService: ReservationService,
    private authService: AuthService,
  ) {}

  ngOnInit() {
    this.loadMatchs();
  }

  loadMatchs() {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      this.error.set('Vous devez être connecté pour consulter les matchs.');
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
        this.error.set(getApiErrorMessage(err, 'Impossible de charger les matchs publics disponibles.'));
      },
    });
  }

  rejoindreMatch(match: any) {
    const matricule = this.authService.getMatricule();

    if (!matricule) {
      this.error.set('Vous devez être connecté pour réserver.');
      return;
    }

    if (!this.peutReserver(match)) {
      this.error.set(this.raisonBlocageReservation(match));
      return;
    }

    if (this.joiningMatchId() !== null) {
      return;
    }

    this.error.set(null);
    this.success.set(null);
    this.joiningMatchId.set(match.id);

    this.reservationService
      .rejoindreMatch({
        matricule: matricule,
        matchId: match.id,
      })
      .pipe(finalize(() => this.joiningMatchId.set(null)))
      .subscribe({
        next: () => {
          this.success.set('Réservation en attente de paiement.');
          this.loadMatchs();
        },
        error: (err) => {
          this.error.set(getApiErrorMessage(err, 'Réservation impossible'));
        },
      });
  }

  payerMatchPublic(match: any) {
    const matricule = this.authService.getMatricule();
    if (!matricule) {
      this.error.set('Vous devez être connecté pour réserver.');
      return;
    }

    if (!this.peutReserver(match)) {
      this.error.set(this.raisonBlocageReservation(match));
      return;
    }

    if (this.joiningMatchId() !== null) {
      return;
    }

    this.error.set(null);
    this.success.set(null);
    this.joiningMatchId.set(match.id);

    this.reservationService.rejoindreMatch({ matricule: matricule, matchId: match.id }).pipe(
      switchMap((reservation: any) => this.reservationService.payerReservation(reservation.id)),
      finalize(() => this.joiningMatchId.set(null)),
    ).subscribe({
      next: () => {
        this.success.set('Le paiement a été enregistré.');
        this.loadMatchs();
      },
      error: (err) => {
        this.error.set(getApiErrorMessage(err, 'Paiement impossible'));
        this.loadMatchs();
      },
    });
  }

  ouvrirPaiement(match: any) {
    if (!this.peutReserver(match)) {
      this.error.set(this.raisonBlocageReservation(match));
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
