import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
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
})
export class MatchListComponent implements OnInit {
  matchs: any[] = [];

  constructor(
    private matchService: MatchService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.matchService.getMatchDisponibles().subscribe((data) => {
      console.log('MATCHS:', data);

      this.matchs = data;
      this.cdr.detectChanges();
    });
  }

  rejoindreMatch(match: any) {
    console.log('Match sélectionné:', match);
  }
}
