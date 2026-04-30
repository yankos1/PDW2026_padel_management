import { Component, OnInit, signal } from '@angular/core';
import { MatchService } from '../../services/match.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatError, MatFormField, MatInput, MatLabel } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatButton } from '@angular/material/button';
import { Terrain } from '../../models/terrain';
import { MatCard } from '@angular/material/card';

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
  date: Date | null = null;
  slots: string[] = [];
  heureSelected: string = '';
  terrainId: number | null = null;
  isPublic: boolean = true;
  terrains = signal<Terrain[]>([]);

  error = signal<string | null>(null);

  constructor(
    private matchService: MatchService,
    private router: Router,
  ) {}

  ngOnInit() {
    console.log('INIT OK');
    this.matchService.getTerrains().subscribe({
      next: (data) => {
        console.log('terrain:', data);
        this.terrains.set(data);
      },
      error: () => this.error.set('Erreur chargement de terrains'),
    });
  }

  createMatch() {
    if (!this.date || !this.heureSelected || !this.terrainId) {
      this.error.set('Tous les champs sont obligatoires');
      return;
    }

    //construire datetime et convertir heure europe
    const dateTime = `${this.date}T${this.heureSelected}:00`;
    const dto = {
      organisateur_matricule: 'G0001', //TODO a changer, juste pour test
      terrainID: this.terrainId,
      date: dateTime,
      estPublic: this.isPublic,
    };

    console.log('DTO:', dto);

    this.matchService.createMatch(dto).subscribe({
      next: () => this.router.navigate(['/mes-reservations']),
      error: (err) =>
        this.error.set(err.error?.message || err.error || 'Erreur lors de la création'),
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

  onTerrainChange() {
    const terrain = this.terrains().find((t) => t.id === this.terrainId);
    console.log("terrain selectionné :",terrain);

    if (terrain) {
      this.generateSlots(terrain.heureOuverture, terrain.heureFermeture);
    }else {
      console.log("pas de site ou heures")
    }
  }

  add90min(time: string): string {
    const date = new Date(`1970-01-01T${time}`);
    date.setMinutes(date.getMinutes() + 90);
    return date.toTimeString().slice(0, 5);
  }

  onDateChange() {
    if (!this.date) return;

    const dateStr = new Date(this.date).toISOString().split('T')[0];

    this.matchService.getCreneauxDisponibles(dateStr).subscribe({
      next: (data) => this.slots = data,
      error: (err) => {
        console.error("ERREUR BACK:", err);
        this.error.set(err.error?.message || "Erreur chargement terrains");
      }
    });
  }

  onSlotChange() {
    if (!this.date || !this.heureSelected) return;

    const dateStr = new Date(this.date).toISOString().split('T')[0];

    this.matchService.getTerrainsDisponiblesParCreneau(dateStr, this.heureSelected)
      .subscribe({
        next: (data) => this.terrains.set(data),
        error: () => this.error.set("Erreur chargement terrains")
      });
  }
}
