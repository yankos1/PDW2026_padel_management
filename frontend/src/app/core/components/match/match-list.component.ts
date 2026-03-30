import {Component, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatchService} from '../../services/match.service';
import {MatDivider} from '@angular/material/list';
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {ReservationService} from '../../services/reservation.service';
import {AuthService} from '../../services/auth.service';

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

  constructor(private matchService: MatchService, private reservationService: ReservationService, private authService: AuthService) {}

  ngOnInit() {
    this.matchService.getMatchDisponibles().subscribe((data) => {
      console.log('MATCHS:', data);

      this.matchs.set(data);
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
    this.reservationService.rejoindreMatch({
      matricule: matricule,
      matchId: match.id
    }).subscribe({
      next: (res) => {
        console.log('réservation réussir',res);

        this.matchService.getMatchDisponibles().subscribe((data) => {
          this.matchs.set(data);
          alert('Tu as rejoint le match ');
        });
      },
      error: (err) => {
        console.log(err)
        const message = err.error;
        alert(message);
      }
    });
  }
}
