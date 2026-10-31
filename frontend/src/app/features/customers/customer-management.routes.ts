import {Routes} from '@angular/router';

/**
 * Routes for Customer operations available to all bank roles.
 *
 * The route group is mounted below `/customers` by the root configuration and
 * inherits its role guard.
 */
export const customerManagementRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/customer-search/customer-search')
        .then((module) => module.CustomerSearch),
  },
];
