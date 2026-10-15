import {Routes} from '@angular/router';
import {authGuard} from './core/auth/guards/auth-guard';
import {roleGuard, ROUTE_ROLES,} from './core/auth/guards/role-guard';

export const routes: Routes = [
  {
    path: 'protected',
    canActivate: [authGuard],

    loadComponent: () =>
      import('./pages/protected/protected')
        .then((module) => module.Protected),
  },
  {
    path: 'admin',
    canActivate: [roleGuard],

    data: {
      [ROUTE_ROLES]: ['ADMIN'],
    },

    loadComponent: () =>
      import('./pages/protected/protected')
        .then((module) => module.Protected),
  },
  {
    path: 'forbidden',

    loadComponent: () =>
      import('./pages/forbidden/forbidden')
        .then((module) => module.Forbidden),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
