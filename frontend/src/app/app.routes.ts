import { Routes } from '@angular/router';
import { LoginComponent } from './core/components/login/login.component';
import { MatchListComponent } from './core/components/match/match-list.component';

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
  },
];
