import { Component } from '@angular/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ReservationService } from '../../services/reservation.service';
import { getApiErrorMessage } from '../../utils/api-error.util';

@Component({
  selector: 'app-reservation',
  imports: [],
  templateUrl: './reservation.html',
  styleUrl: './reservation.css',
})
export class Reservation {
  submitting = false;
  error = '';
  success = '';

  constructor(private reservationService: ReservationService, private authService: AuthService) {}

  rejoindreMatch(match: any) {
    if (this.submitting) {
      return;
    }

    const user = this.authService.getUser();

    if (!user) {
      this.error = 'Vous devez être connecté pour réserver.';
      return;
    }

    this.error = '';
    this.success = '';
    this.submitting = true;

    const input = {
      matricule: user.matricule,
      matchId: match.id,
    };

    this.reservationService.rejoindreMatch(input)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.success = 'Inscription réussie.';
          this.loadMatchs();
        },
        error: (err) => {
          this.error = getApiErrorMessage(err, 'Inscription impossible');
        },
      });
  }

  private loadMatchs() {}
}
