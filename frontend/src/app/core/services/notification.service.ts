import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

type NotificationType = 'success' | 'error' | 'info' | 'warning';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly durations: Record<NotificationType, number> = {
    success: 3000,
    info: 4000,
    warning: 4000,
    error: 5000,
  };

  constructor(private snackBar: MatSnackBar) {}

  success(message: string) {
    this.open(message, 'success');
  }

  error(message: string) {
    this.open(message, 'error');
  }

  info(message: string) {
    this.open(message, 'info');
  }

  warning(message: string) {
    this.open(message, 'warning');
  }

  private open(message: string, type: NotificationType) {
    const config: MatSnackBarConfig = {
      duration: this.durations[type],
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['app-snackbar', `app-snackbar-${type}`],
    };

    this.snackBar.open(message, 'Fermer', config);
  }
}
