import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatSidenav, MatSidenavContainer, MatSidenavContent } from '@angular/material/sidenav';

@Component({
  selector: 'app-sidebar',
  imports: [
    MatButton,
    RouterLink,
    MatSidenavContainer,
    MatSidenav,
    MatSidenavContent,
    RouterOutlet,
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  user: any;

  constructor(private authService: AuthService) {
    this.user = this.authService.getUser();
  }

  logout() {
    this.authService.logout();
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('user');
  }
}
