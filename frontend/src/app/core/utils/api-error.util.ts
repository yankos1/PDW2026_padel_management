import { HttpErrorResponse } from '@angular/common/http';
import { ErrorResponse } from '../models/error-response';

export function getApiErrorMessage(error: unknown, fallback = 'Une erreur est survenue'): string {
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'Le serveur est inaccessible';
  }

  const apiError = getErrorResponse(error);
  if (apiError?.message) {
    return apiError.message;
  }

  if (typeof getHttpErrorBody(error) === 'string') {
    return getHttpErrorBody(error) as string;
  }

  return fallback;
}

export function getApiFieldErrors(error: unknown): Record<string, string> {
  return getErrorResponse(error)?.fieldErrors ?? {};
}

function getErrorResponse(error: unknown): ErrorResponse | null {
  const body = getHttpErrorBody(error);

  if (!isRecord(body)) {
    return null;
  }

  const fieldErrors = body['fieldErrors'];

  return {
    status: typeof body['status'] === 'number' ? body['status'] : 0,
    message: typeof body['message'] === 'string' ? body['message'] : '',
    timestamp: typeof body['timestamp'] === 'string' ? body['timestamp'] : '',
    fieldErrors: isStringRecord(fieldErrors) ? fieldErrors : {},
  };
}

function getHttpErrorBody(error: unknown): unknown {
  if (error instanceof HttpErrorResponse) {
    return error.error;
  }

  if (isRecord(error) && 'error' in error) {
    return error['error'];
  }

  return null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isStringRecord(value: unknown): value is Record<string, string> {
  return isRecord(value) && Object.values(value).every((entry) => typeof entry === 'string');
}
