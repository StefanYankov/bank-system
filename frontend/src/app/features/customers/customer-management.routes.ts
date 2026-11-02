import {Routes} from '@angular/router';

import {roleGuard, ROUTE_ROLES} from '../../core/auth/guards/role-guard';

export const customerManagementRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/customer-search/customer-search')
        .then((module) => module.CustomerSearch),
  },
  {
    path: 'individuals/register',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: ['CLERK', 'ADMIN'],
    },
    loadComponent: () =>
      import(
        './pages/individual-customer-registration/individual-customer-registration'
        ).then((module) => module.IndividualCustomerRegistration),
  },
  {
    path: 'individuals/:id',
    loadComponent: () =>
      import(
        './pages/individual-customer-details/individual-customer-details'
        ).then((module) => module.IndividualCustomerDetails),
  },
];
