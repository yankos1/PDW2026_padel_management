import { Component, signal } from '@angular/core';
import { Navbar } from './core/components/navbar/navbar';

@Component({
  selector: 'app-root',
  imports: [Navbar],
  templateUrl: './app.html',
  styleUrl: './app.css',
  standalone: true,
})
export class App {
  protected readonly title = signal('frontend');

  //TO DO later
  get isLoggedIn(): boolean {
    return !!localStorage.getItem('user');
  }
}
