import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { MonCompte } from './mon-compte';

describe('MonCompte', () => {
  it('exposes password change only for administrators', () => {
    expect(createComponent({ isAdmin: vi.fn().mockReturnValue(true) }).isAdmin()).toBe(true);
    expect(createComponent({ isAdmin: vi.fn().mockReturnValue(false) }).isAdmin()).toBe(false);
  });

  it('rejects a short password without calling the backend', () => {
    const changeAdminPassword = vi.fn().mockReturnValue(of(undefined));
    const component = createComponent({ changeAdminPassword });
    component.currentPassword = 'current-password';
    component.newPassword = 'short';
    component.confirmPassword = 'short';

    component.changePassword();

    expect(changeAdminPassword).not.toHaveBeenCalled();
    expect(component.passwordError).toContain('12 caractères');
  });

  it('rejects a different confirmation', () => {
    const changeAdminPassword = vi.fn().mockReturnValue(of(undefined));
    const component = createComponent({ changeAdminPassword });
    component.currentPassword = 'current-password';
    component.newPassword = 'new-secure-password';
    component.confirmPassword = 'different-password';

    component.changePassword();

    expect(changeAdminPassword).not.toHaveBeenCalled();
    expect(component.passwordError).toBe('Les mots de passe ne correspondent pas.');
  });

  it('calls PUT service, reports success and clears all password fields', () => {
    const notificationService = notificationMock();
    const changeAdminPassword = vi.fn().mockReturnValue(of(undefined));
    const component = createComponent({ changeAdminPassword }, notificationService);
    component.currentPassword = 'current-password';
    component.newPassword = 'new-secure-password';
    component.confirmPassword = 'new-secure-password';

    component.changePassword();

    expect(changeAdminPassword).toHaveBeenCalledWith(
      'current-password',
      'new-secure-password',
      'new-secure-password',
    );
    expect(notificationService.success).toHaveBeenCalledWith('Mot de passe modifié avec succès.');
    expect(component.currentPassword).toBe('');
    expect(component.newPassword).toBe('');
    expect(component.confirmPassword).toBe('');
  });

  it('shows the generic backend error for a wrong current password', () => {
    const notificationService = notificationMock();
    const changeAdminPassword = vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 401,
      error: { message: 'Identifiants invalides' },
    })));
    const component = createComponent({ changeAdminPassword }, notificationService);
    component.currentPassword = 'wrong-current-password';
    component.newPassword = 'new-secure-password';
    component.confirmPassword = 'new-secure-password';

    component.changePassword();

    expect(component.passwordError).toBe('Identifiants invalides');
    expect(notificationService.error).toHaveBeenCalledWith('Identifiants invalides');
  });
});

function createComponent(authOverrides: Record<string, any> = {}, notificationService = notificationMock()) {
  const authService = {
    getUser: vi.fn().mockReturnValue({ matricule: 'G0001', role: 'ADMIN_GLOBAL' }),
    getTypeMembre: vi.fn().mockReturnValue('GLOBAL'),
    getDelaiReservation: vi.fn().mockReturnValue(21),
    getMatricule: vi.fn().mockReturnValue('G0001'),
    isAdmin: vi.fn().mockReturnValue(true),
    changeAdminPassword: vi.fn().mockReturnValue(of(undefined)),
    ...authOverrides,
  };
  return new MonCompte(authService as any, notificationService as any);
}

function notificationMock() {
  return {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  };
}
