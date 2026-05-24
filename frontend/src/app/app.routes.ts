import { Routes } from '@angular/router';
import { LoginComponent } from './core/components/login/login.component';
import { MatchListComponent } from './core/components/match-list/match-list.component';
import { authGuard } from './core/guards/auth-guard';
import {Reservation} from './core/components/reservation/reservation';
import { MesReservations } from './core/components/mes-reservations/mes-reservations';
import { CreateMatch } from './core/components/create-match/create-match';
import { Admin } from './core/components/admin/admin';
import { Home } from './core/components/home/home';
import { MonCompte } from './core/components/mon-compte/mon-compte';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'home',
    component: Home,
    canActivate: [authGuard]
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
    canActivate: [authGuard]
  },
  {
    path: 'mon-compte',
    component: MonCompte,
    canActivate: [authGuard]
  },
  {
    path: 'create-match',
    component: CreateMatch,
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    component: Admin,
    canActivate: [authGuard]
  }
];
