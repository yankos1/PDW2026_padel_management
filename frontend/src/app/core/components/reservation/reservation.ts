import { Component } from '@angular/core';
import { ReservationService } from '../../services/reservation.service';
import { AuthService } from '../../services/auth.service';
import { getApiErrorMessage } from '../../utils/api-error.util';


@Component({
  selector: 'app-reservation',
  imports: [],
  templateUrl: './reservation.html',
  styleUrl: './reservation.css',
})
export class Reservation {
  constructor(private reservationService: ReservationService, private authService: AuthService) {
  }

  rejoindreMatch(match:any) {
    const user = this.authService.getUser();

    if (!user) {
      alert('Pas de matricule');
      return;
    }

    const input={
      matricule: user.matricule,
      matchId: match.id
    };

    this.reservationService.rejoindreMatch(input)
      .subscribe({
        next:(res) =>{
          console.log('Réservation crée',res);
          alert("Inscription réussie");
          this.loadMatchs();
        },
        error:(err) =>{
          console.error(err);
          alert(getApiErrorMessage(err, 'Inscription impossible'));
        }
      });
  }

  private loadMatchs() {

  }
}
