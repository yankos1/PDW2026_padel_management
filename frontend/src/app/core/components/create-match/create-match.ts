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
  heure: string = '';
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
    if (!this.date || !this.heure || !this.terrainId) {
      this.error.set('Tous les champs sont obligatoires');
      return;
    }

    //construire datetime et convertir heure europe
    const dateTime = `${this.date}T${this.heure}:00`;
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
}
