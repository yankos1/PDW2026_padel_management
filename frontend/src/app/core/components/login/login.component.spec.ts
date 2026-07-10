import { HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  it('shows backend invalid credentials message', () => {
    const authService = {
      login: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 401,
        error: {
          status: 401,
          message: 'Identifiants invalides',
          timestamp: '2026-07-10T18:00:00Z',
          fieldErrors: {},
        },
      }))),
      setUser: vi.fn(),
    };
    const router = { navigate: vi.fn() };
    const component = new LoginComponent(authService as any, router as any);

    component.matricule = 'G9999';
    component.login();

    expect(component.error).toBe('Identifiants invalides');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
