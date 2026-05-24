import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  matricule = '';
  password = '';
  error = '';
  success = '';
  adminPasswordRequired = false;
  adminPasswordCreation = false;
  showRegisterForm = false;
  registerLoading = false;
  private matriculeStatusTimer?: ReturnType<typeof setTimeout>;
  registerForm = {
    nom: '',
    prenom: '',
    email: '',
  };

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  login() {
    this.error = '';
    this.success = '';

    if (this.adminPasswordRequired && !this.password) {
      this.error = this.adminPasswordCreation
        ? 'Choisissez un mot de passe admin'
        : 'Mot de passe admin requis';
      return;
    }

    const password = this.adminPasswordRequired ? this.password : undefined;

    this.authService.login(this.matricule, password).subscribe({
      next: (user) => {
        console.log('utilisateur connecte:', user);
        this.authService.setUser(user);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        const message = typeof err.error === 'string' ? err.error : 'Connexion impossible';
        if (message === 'Mot de passe admin requis' || message === 'Creation du mot de passe admin requise') {
          this.adminPasswordRequired = true;
          this.checkAdminPasswordStatus();
          return;
        }

        this.error = message;
      },
    });
  }

  onMatriculeChange() {
    if (this.matriculeStatusTimer) {
      clearTimeout(this.matriculeStatusTimer);
    }

    this.adminPasswordRequired = false;
    this.adminPasswordCreation = false;
    this.password = '';
    this.error = '';
    this.success = '';

    const matricule = this.matricule.trim();

    if (matricule.length < 2) {
      return;
    }

    this.matriculeStatusTimer = setTimeout(() => this.checkAdminPasswordStatus(), 300);
  }

  checkAdminPasswordStatus() {
    const matricule = this.matricule.trim();

    if (!matricule) {
      return;
    }

    this.authService.getAdminPasswordStatus(matricule).subscribe({
      next: (status) => {
        this.adminPasswordRequired = status.admin;
        this.adminPasswordCreation = status.passwordCreation;
      },
      error: () => {
        this.adminPasswordRequired = false;
        this.adminPasswordCreation = false;
      },
    });
  }

  toggleRegisterForm() {
    this.showRegisterForm = !this.showRegisterForm;
    this.error = '';
    this.success = '';
  }

  register() {
    if (this.registerLoading) {
      return;
    }

    this.error = '';
    this.success = '';

    const input = {
      prenom: this.registerForm.prenom.trim(),
      nom: this.registerForm.nom.trim(),
      email: this.registerForm.email.trim(),
    };

    if (!input.prenom || !input.nom || !input.email) {
      this.error = 'Completez tous les champs pour creer votre compte.';
      return;
    }

    this.registerLoading = true;

    this.authService.register(input).subscribe({
      next: (user) => {
        this.matricule = user.matricule;
        this.password = '';
        this.adminPasswordRequired = false;
        this.adminPasswordCreation = false;
        this.registerForm = {
          nom: '',
          prenom: '',
          email: '',
        };
        this.showRegisterForm = false;
        this.registerLoading = false;
        this.success = `Compte cree! Votre matricule est ${user.matricule}, vous pouvez vous connecter.`;
      },
      error: (err) => {
        this.registerLoading = false;
        this.error = typeof err.error === 'string' ? err.error : 'Creation du compte impossible';
      },
    });
  }
}
