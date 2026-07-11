import { Component } from '@angular/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ReservationService } from '../../services/reservation.service';
import { getApiErrorMessage } from '../../utils/api-error.util';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-reservation',
  imports: [],
  templateUrl: './reservation.html',
  styleUrl: './reservation.css',
})
export class Reservation {
  submitting = false;

  constructor(
    private reservationService: ReservationService,
    private authService: AuthService,
    private notificationService: NotificationService,
  ) {}

  rejoindreMatch(match: any) {
    if (this.submitting) {
      return;
    }

    const user = this.authService.getUser();

    if (!user) {
      this.notificationService.warning('Vous devez être connecté pour réserver.');
      return;
    }

    this.submitting = true;

    const input = {
      matricule: user.matricule,
      matchId: match.id,
    };

    this.reservationService.rejoindreMatch(input)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.notificationService.success('Réservation confirmée avec succès.');
          this.loadMatchs();
        },
        error: (err) => {
          this.notificationService.error(getApiErrorMessage(err, 'Inscription impossible.'));
        },
      });
  }

  private loadMatchs() {}
}
