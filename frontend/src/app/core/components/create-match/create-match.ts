import { Component, OnInit, signal } from '@angular/core';
import { MatchService } from '../../services/match.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatError, MatFormField, MatInput, MatLabel } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatButton } from '@angular/material/button';
import { Terrain } from '../../models/terrain';
import { MatCard } from '@angular/material/card';
import { Site } from '../../models/site';
import { AuthService } from '../../services/auth.service';
import { finalize } from 'rxjs';
import { getApiErrorMessage, getApiFieldErrors } from '../../utils/api-error.util';

@Component({
  selector: 'app-create-match',
  standalone: true,
  imports: [
    FormsModule,
    MatFormField,
    MatLabel,
    MatSelect,
    MatOption,
    MatButton,
    MatInput,
    MatCard,
    MatError,
  ],
  templateUrl: './create-match.html',
  styleUrl: './create-match.css',
})
export class CreateMatch implements OnInit {
  date: string | null = null;
  slots: string[] = [];
  heureSelected: string = '';
  terrainId: number | null = null;
  isPublic: boolean = true;
  terrains = signal<Terrain[]>([]);
  sites = signal<Site[]>([]);
  siteId: number | null = null;

  error = signal<string | null>(null);
  success = signal<string | null>(null);
  fieldErrors = signal<Record<string, string>>({});
  loadingSites = signal(false);
  loadingSlots = signal(false);
  loadingTerrains = signal(false);
  submitting = signal(false);

  constructor(
    private matchService: MatchService,
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit() {
    this.loadSites();
  }

  loadSites() {
    this.error.set(null);
    this.loadingSites.set(true);

    this.matchService.getSites()
      .pipe(finalize(() => this.loadingSites.set(false)))
      .subscribe({
      next: (data) => {
        this.sites.set(data);
      },
      error: (err) => this.error.set(getApiErrorMessage(err, 'Erreur chargement des sites')),
    });
  }

  createMatch() {
    if (this.submitting()) {
      return;
    }

    this.error.set(null);
    this.success.set(null);
    this.fieldErrors.set({});

    if (!this.date || !this.heureSelected || !this.terrainId) {
      this.error.set('Tous les champs sont obligatoires');
      return;
    }

    const matricule = this.authService.getMatricule();

    if (!matricule) {
      this.error.set('Vous devez être connecté pour créer un match');
      return;
    }

    const dateTime = `${this.date}T${this.heureSelected}:00`;

    if (!this.authService.canReserveDate(dateTime)) {
      this.error.set(this.messageDelaiReservation());
      return;
    }

    if (!this.siteAutorise(this.siteId)) {
      this.error.set('Un membre du site ne peut réserver que sur son site');
      return;
    }

    const dto = {
      organisateur_matricule: matricule,
      terrainID: this.terrainId,
      date: dateTime,
      estPublic: this.isPublic,
    };

    this.submitting.set(true);

    this.matchService.createMatch(dto)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
      next: () => {
        this.success.set('Le match a été créé.');
        setTimeout(() => this.router.navigate(['/mes-reservations']), 700);
      },
      error: (err) => {
        this.fieldErrors.set(getApiFieldErrors(err));
        this.error.set(getApiErrorMessage(err, 'Erreur lors de la création'));
      },
    });
  }

  generateSlots(open: string, close: string) {
    this.slots = [];

    let current = new Date(`1970-01-01T${open}`);
    const end = new Date(`1970-01-01T${close}`);

    while (true) {
      const slotEnd = new Date(current.getTime() + 90 * 60000);

      if (slotEnd > end) break;

      this.slots.push(this.formatTime(current));

      // 1h30 + 15 min
      current = new Date(current.getTime() + (90 + 15) * 60000);
    }
  }

  formatTime(date: Date): string {
    return date.toTimeString().slice(0, 5);
  }

  estDansLes24h(dateTime: string): boolean {
    const dateMatch = new Date(dateTime);
    const maintenant = new Date();
    const dans24h = new Date(maintenant.getTime() + 24 * 60 * 60 * 1000);

    return dateMatch <= dans24h;
  }

  matchPriveDesactive(): boolean {
    if (!this.date || !this.heureSelected) {
      return false;
    }

    return this.estDansLes24h(`${this.date}T${this.heureSelected}:00`);
  }

  appliquerDisponibiliteTypeMatch() {
    if (this.matchPriveDesactive()) {
      this.isPublic = true;
    }
  }

  onTerrainChange() {
    const terrain = this.terrains().find((t) => t.id === this.terrainId);

    if (terrain) {
      this.generateSlots(terrain.heureOuverture, terrain.heureFermeture);
    }
  }

  add90min(time: string): string {
    const date = new Date(`1970-01-01T${time}`);
    date.setMinutes(date.getMinutes() + 90);
    return date.toTimeString().slice(0, 5);
  }

  onDateChange() {
    if (!this.date) return;

    const dateMax = this.authService.getDateMaxReservation();

    if (dateMax && (this.date < this.authService.getDateMinReservation() || this.date > dateMax)) {
      this.error.set(this.messageDelaiReservation());
      this.resetChoixReservation();
      return;
    }

    this.error.set(null);
    this.appliquerDisponibiliteTypeMatch();
    this.loadingSlots.set(true);

    this.matchService.getCreneauxDisponibles(this.date)
      .pipe(finalize(() => this.loadingSlots.set(false)))
      .subscribe({
      next: (data) => (this.slots = data),
      error: (err) => {
        this.error.set(getApiErrorMessage(err, 'Erreur chargement terrains'));
      },
    });
  }

  onSlotChange() {
    this.appliquerDisponibiliteTypeMatch();

    if (!this.date || !this.heureSelected || !this.siteId) {
      return;
    }

    this.terrainId = null;
    this.loadingTerrains.set(true);

    this.matchService
      .getTerrainsDisponiblesParCreneau(this.date, this.heureSelected, this.siteId)
      .pipe(finalize(() => this.loadingTerrains.set(false)))
      .subscribe({
        next: (data) => {
          this.terrains.set(data);
        },
        error: (err) => {
          this.error.set(getApiErrorMessage(err, 'Erreur chargement terrains'));
        },
      });
  }

  protected onSiteChange() {
    const site = this.sites().find((s) => s.id === this.siteId);

    if (!site) return;

    if (!this.siteAutorise(site.id)) {
      this.error.set('Un membre du site ne peut réserver que sur son site');
      this.resetChoixReservation(false);
      return;
    }

      this.error.set(null);

      // reset
      this.terrainId = null;
      this.heureSelected = '';
      this.slots = [];
      this.terrains.set([]);


      // générer les créneaux selon les horaires du site
      this.generateSlots(site.heureOuverture, site.heureFermeture);
    }

  get dateMin(): string {
    return this.authService.getDateMinReservation();
  }

  get dateMax(): string | null {
    return this.authService.getDateMaxReservation();
  }

  siteAutorise(siteId: number | null): boolean {
    if (this.authService.getTypeMembre() !== 'SITE') {
      return true;
    }

    const siteMembreId = this.authService.getSiteMembreId();

    if (siteMembreId === null) {
      return true;
    }

    return siteId !== null && siteId === siteMembreId;
  }

  messageDelaiReservation(): string {
    const delai = this.authService.getDelaiReservation();

    if (delai === null) {
      return 'Catégorie de membre invalide';
    }

    return `Vous pouvez réserver au maximum ${delai} jours avant la date du match`;
  }

  private resetChoixReservation(resetSite = true) {
    if (resetSite) {
      this.siteId = null;
    }

    this.terrainId = null;
    this.heureSelected = '';
    this.slots = [];
    this.terrains.set([]);
    this.appliquerDisponibiliteTypeMatch();
  }

  fieldError(field: string): string {
    return this.fieldErrors()[field] ?? '';
  }
}
