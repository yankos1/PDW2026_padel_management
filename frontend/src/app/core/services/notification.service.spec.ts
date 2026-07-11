import { vi } from 'vitest';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  it('shows a success notification with the shared snackbar config', () => {
    const snackBar = { open: vi.fn() };
    const service = new NotificationService(snackBar as any);

    service.success('Compte créé avec succès.');

    expect(snackBar.open).toHaveBeenCalledWith(
      'Compte créé avec succès.',
      'Fermer',
      expect.objectContaining({
        duration: 3000,
        panelClass: ['app-snackbar', 'app-snackbar-success'],
      }),
    );
  });

  it('shows an error notification for longer', () => {
    const snackBar = { open: vi.fn() };
    const service = new NotificationService(snackBar as any);

    service.error('Une erreur est survenue. Veuillez réessayer.');

    expect(snackBar.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez réessayer.',
      'Fermer',
      expect.objectContaining({
        duration: 5000,
        panelClass: ['app-snackbar', 'app-snackbar-error'],
      }),
    );
  });
});
