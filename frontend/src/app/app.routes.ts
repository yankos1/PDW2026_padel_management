import { Routes } from '@angular/router';
import { LoginComponent } from './core/components/login/login.component';
import { MatchListComponent } from './core/components/match-list/match-list.component';
import { authGuard } from './core/guards/auth-guard';
import {Reservation} from './core/components/reservation/reservation';
import { MesReservations } from './core/components/mes-reservations/mes-reservations';
import { CreateMatch } from './core/components/create-match/create-match';
import { Admin } from './core/components/admin/admin';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'match',
    component: MatchListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'reservation',
    component: Reservation,
  },
  {
    path: 'mes-reservations',
    component: MesReservations,
  },
  {
    path: 'create-match',
    component: CreateMatch,
  },
  {
    path: 'admin',
    component: Admin,
  }
];
