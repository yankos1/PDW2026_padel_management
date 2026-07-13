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
import { NotificationService } from '../../services/notification.service';

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

  loginLoading = false;
  adminPasswordRequired = false;
  showRegisterForm = false;
  registerLoading = false;
  registerSubmitted = false;
  registerFieldErrors: Record<string, string> = {};
  registerForm = {
    nom: '',
    prenom: '',
    email: '',
  };

  constructor(
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService,
  ) {}

  login() {
    if (this.loginLoading) {
      return;
    }

    this.error = '';


    if (!this.matricule.trim()) {
      this.error = 'Le matricule est obligatoire.';
      this.notificationService.warning(this.error);
      return;
    }

    if (this.adminPasswordRequired && !this.password.trim()) {
      this.error = 'Mot de passe admin requis';
      this.notificationService.warning(this.error);
      return;
    }

    const password = this.adminPasswordRequired ? this.password : undefined;
    this.loginLoading = true;

    this.authService.login(this.matricule.trim(), password)
      .pipe(finalize(() => (this.loginLoading = false)))
      .subscribe({
      next: (session) => {
        this.authService.setUser(session);
        this.notificationService.success('Connexion réussie.');
        this.router.navigate(['/home']);
      },
      error: (err) => {
        const message = this.errorMessage(err, 'Connexion impossible');
        if (!this.adminPasswordRequired) {
          this.adminPasswordRequired = true;
          this.error = message;
          this.notificationService.error(message);
          return;
        }

        this.error = message;
        this.notificationService.error(message);
      },
    });
  }

  onMatriculeChange() {
    this.adminPasswordRequired = false;
    this.password = '';
    this.error = '';
  }

  toggleRegisterForm() {
    this.showRegisterForm = !this.showRegisterForm;
    this.error = '';

    this.registerFieldErrors = {};
    this.registerSubmitted = false;
  }

  register() {
    if (this.registerLoading) {
      return;
    }

    this.error = '';

    this.registerFieldErrors = {};
    this.registerSubmitted = true;

    const input = {
      prenom: this.registerForm.prenom.trim(),
      nom: this.registerForm.nom.trim(),
      email: this.registerForm.email.trim(),
    };

    if (!input.prenom || !input.nom || !input.email) {
      this.error = 'Complétez tous les champs pour créer votre compte.';
      this.notificationService.warning(this.error);
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
        this.registerForm = {
          nom: '',
          prenom: '',
          email: '',
        };
        this.showRegisterForm = false;
        this.notificationService.success(`Compte créé avec succès. Votre matricule est ${user.matricule}.`);
      },
      error: (err) => {
        this.registerFieldErrors = getApiFieldErrors(err);
        this.error = this.errorMessage(err, 'Création du compte impossible');
        this.notificationService.error(this.error);
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
