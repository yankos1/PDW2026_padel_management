import { HttpErrorResponse } from '@angular/common/http';
import { getApiErrorMessage, getApiFieldErrors } from './api-error.util';

describe('api-error.util', () => {
  it('reads backend error response messages', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        message: 'Données invalides',
        timestamp: '2026-07-10T18:00:00Z',
        fieldErrors: {},
      },
    });

    expect(getApiErrorMessage(error, 'Fallback')).toBe('Données invalides');
  });

  it('uses fallback when the backend response has no message', () => {
    const error = new HttpErrorResponse({
      status: 500,
      error: { unexpected: true },
    });

    expect(getApiErrorMessage(error, 'Fallback')).toBe('Fallback');
  });

  it('returns server unavailable for network errors', () => {
    const error = new HttpErrorResponse({ status: 0 });

    expect(getApiErrorMessage(error, 'Fallback')).toBe('Le serveur est inaccessible');
  });

  it('reads backend field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        message: 'Données invalides',
        timestamp: '2026-07-10T18:00:00Z',
        fieldErrors: {
          matricule: 'Matricule invalide',
        },
      },
    });

    expect(getApiFieldErrors(error)).toEqual({ matricule: 'Matricule invalide' });
  });
});
