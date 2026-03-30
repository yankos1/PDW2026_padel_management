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
  ],
  templateUrl: './mes-reservations.html',
  styleUrl: './mes-reservations.css',
})
export class MesReservations {
  reservations = signal<any>([]);

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
}
