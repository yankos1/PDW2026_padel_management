import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatchListComponent } from './core/components/match/match-list.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatchListComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');
}
