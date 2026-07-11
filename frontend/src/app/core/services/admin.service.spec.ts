import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calls dashboard endpoint with filters', () => {
    service.getDashboard({
      dateDebut: '2026-01-01',
      dateFin: '2026-01-31',
      siteId: 1,
      terrainId: 10,
    }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/admin/dashboard');

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('dateDebut')).toBe('2026-01-01');
    expect(request.request.params.get('dateFin')).toBe('2026-01-31');
    expect(request.request.params.get('siteId')).toBe('1');
    expect(request.request.params.get('terrainId')).toBe('10');

    request.flush({
      resume: {
        chiffreAffaires: 0,
        nombreMatchs: 0,
        reservationsConfirmees: 0,
        tauxRemplissageMatchs: 0,
        tauxOccupationTerrains: 0,
        soldesDus: 0,
        membresActifs: 0,
        matchsAnnules: 0,
        matchsProchainsIncomplets: 0,
      },
      chiffreAffairesParMois: [],
      matchsParMois: [],
      tauxRemplissageParTerrain: [],
      repartitionMatchsParStatut: [],
      terrainsLesPlusUtilises: [],
      prochainsMatchsIncomplets: [],
    });
  });
});
