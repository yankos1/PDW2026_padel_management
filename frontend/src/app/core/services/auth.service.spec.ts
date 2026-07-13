import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('changes the authenticated admin password with PUT /auth/password', () => {
    service.changeAdminPassword('current-password', 'new-secure-password', 'new-secure-password')
      .subscribe();

    const request = httpMock.expectOne('/api/auth/password');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      currentPassword: 'current-password',
      newPassword: 'new-secure-password',
      confirmPassword: 'new-secure-password',
    });
    expect(request.request.body.matricule).toBeUndefined();
    request.flush(null);
  });
});
