import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  it('shows backend invalid credentials message', () => {
    const notificationService = notificationMock();
    const component = createComponent({
      login: vi.fn().mockReturnValue(throwError(() => backendError(401, 'Identifiants invalides'))),
    }, notificationService);

    component.matricule = 'G9999';
    component.login();

    expect(component.error).toBe('Identifiants invalides');
    expect(notificationService.error).toHaveBeenCalledWith('Identifiants invalides');
  });

  it('disables login while the request is pending and prevents double submit', () => {
    const login$ = new Subject<any>();
    const login = vi.fn().mockReturnValue(login$);
    const component = createComponent({ login });

    component.matricule = 'G0001';
    component.login();
    component.login();

    expect(component.loginLoading).toBe(true);
    expect(login).toHaveBeenCalledTimes(1);

    login$.next({ matricule: 'G0001' });
    login$.complete();

    expect(component.loginLoading).toBe(false);
  });

  it('shows success notification after registration and clears loading', () => {
    const notificationService = notificationMock();
    const register = vi.fn().mockReturnValue(of({ matricule: 'G1234' }));
    const component = createComponent({ register }, notificationService);

    component.registerForm = { prenom: 'Ada', nom: 'Lovelace', email: 'ada@example.test' };
    component.register();

    expect(component.registerLoading).toBe(false);
    expect(notificationService.success).toHaveBeenCalledWith('Compte créé avec succès. Votre matricule est G1234.');
    expect(component.matricule).toBe('G1234');
  });

  it('maps backend field errors during registration', () => {
    const register = vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        message: 'Email invalide',
        timestamp: '2026-07-10T18:00:00Z',
        fieldErrors: { email: 'Email déjà utilisé' },
      },
    })));
    const component = createComponent({ register });

    component.registerForm = { prenom: 'Ada', nom: 'Lovelace', email: 'ada@example.test' };
    component.register();

    expect(component.error).toBe('Email invalide');
    expect(component.registerFieldError('email')).toBe('Email déjà utilisé');
  });
});

function createComponent(authOverrides: Record<string, any> = {}, notificationService = notificationMock()) {
  const authService = {
    login: vi.fn().mockReturnValue(of({ matricule: 'G0001' })),
    register: vi.fn().mockReturnValue(of({ matricule: 'G1234' })),
    setUser: vi.fn(),
    getAdminPasswordStatus: vi.fn().mockReturnValue(of({ admin: false, passwordCreation: false })),
    ...authOverrides,
  };
  const router = { navigate: vi.fn() };

  return new LoginComponent(authService as any, router as any, notificationService as any);
}

function backendError(status: number, message: string): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    error: {
      status,
      message,
      timestamp: '2026-07-10T18:00:00Z',
      fieldErrors: {},
    },
  });
}

function notificationMock() {
  return {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  };
}
