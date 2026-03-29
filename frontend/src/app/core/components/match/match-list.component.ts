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

  constructor(private matchService: MatchService) {}

  ngOnInit() {
    this.matchService.getMatchDisponibles().subscribe((data) => {
      console.log('MATCHS:', data);

      this.matchs.set(data);
    });
  }

  rejoindreMatch(match: any) {
    console.log('Match sélectionné:', match);
  }
}
