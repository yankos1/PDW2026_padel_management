import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { getApiErrorMessage, getApiFieldErrors } from '../../utils/api-error.util';

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
  loginLoading = false;
  adminPasswordRequired = false;
  adminPasswordCreation = false;
  showRegisterForm = false;
  registerLoading = false;
  registerSubmitted = false;
  registerFieldErrors: Record<string, string> = {};
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
    if (this.loginLoading) {
      return;
    }

    this.error = '';
    this.success = '';

    if (!this.matricule.trim()) {
      this.error = 'Le matricule est obligatoire.';
      return;
    }

    if (this.adminPasswordRequired && !this.password.trim()) {
      this.error = this.adminPasswordCreation
        ? 'Choisissez un mot de passe admin'
        : 'Mot de passe admin requis';
      return;
    }

    const password = this.adminPasswordRequired ? this.password : undefined;
    this.loginLoading = true;

    this.authService.login(this.matricule.trim(), password)
      .pipe(finalize(() => (this.loginLoading = false)))
      .subscribe({
      next: (user) => {
        this.authService.setUser(user);
        this.success = 'Connexion réussie.';
        this.router.navigate(['/home']);
      },
      error: (err) => {
        const message = this.errorMessage(err, 'Connexion impossible');
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
    this.registerFieldErrors = {};
    this.registerSubmitted = false;
  }

  register() {
    if (this.registerLoading) {
      return;
    }

    this.error = '';
    this.success = '';
    this.registerFieldErrors = {};
    this.registerSubmitted = true;

    const input = {
      prenom: this.registerForm.prenom.trim(),
      nom: this.registerForm.nom.trim(),
      email: this.registerForm.email.trim(),
    };

    if (!input.prenom || !input.nom || !input.email) {
      this.error = 'Complétez tous les champs pour créer votre compte.';
      return;
    }

    this.registerLoading = true;

    this.authService.register(input)
      .pipe(finalize(() => (this.registerLoading = false)))
      .subscribe({
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
        this.success = `Compte créé ! Votre matricule est ${user.matricule}, vous pouvez vous connecter.`;
      },
      error: (err) => {
        this.registerFieldErrors = getApiFieldErrors(err);
        this.error = this.errorMessage(err, 'Création du compte impossible');
      },
    });
  }

  registerFieldError(field: string): string {
    return this.registerFieldErrors[field] ?? '';
  }

  private errorMessage(err: unknown, fallback: string): string {
    return getApiErrorMessage(err, fallback);
  }
}
