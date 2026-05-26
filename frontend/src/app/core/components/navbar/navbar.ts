import { Component, HostListener } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatSidenav, MatSidenavContainer, MatSidenavContent } from '@angular/material/sidenav';
import { Membre } from '../../models/membre';

@Component({
  selector: 'app-sidebar',
  imports: [
    MatButton,
    RouterLink,
    MatSidenavContainer,
    MatSidenav,
    MatSidenavContent,
    RouterOutlet,
    RouterLinkActive,
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  standalone: true,
})
export class Navbar {
  isMobile = window.innerWidth <= 768;

  constructor(public authService: AuthService) {}

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth <= 768;
  }

  get user(): Membre | null {
    return this.authService.getUser();
  }

  logout() {
    this.authService.logout();
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  closeMenu(drawer: MatSidenav) {
    if (this.isMobile) {
      drawer.close();
    }
  }
}
