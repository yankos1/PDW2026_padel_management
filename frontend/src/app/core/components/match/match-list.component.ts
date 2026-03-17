import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatchService } from '../../services/match.service';

@Component({
  selector: 'app-match-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './match-list.component.html',
})
export class MatchListComponent {
  matchs: any[] = [];

  constructor(private matchService: MatchService) {}

  ngOnInit() {
    this.matchService.getMatchDisponibles().subscribe((data) => {
      console.log('MATCHS:', data);

      this.matchs = data;
    });
  }
}
