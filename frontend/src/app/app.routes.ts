import {Routes} from '@angular/router';

import {authGuard} from './core/auth/guards/auth-guard';
import {roleGuard, ROUTE_ROLES,} from './core/auth/guards/role-guard';

export const routes: Routes = [
  {
    path: 'protected',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/access/pages/protected/protected')
        .then((module) => module.Protected),
  },
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: ['ADMIN'],
    },
    loadChildren: () =>
      import(
        './features/employees/employee-management.routes'
        ).then(
        (module) => module.employeeManagementRoutes,
      ),
  },
  {
    path: 'customers',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: [
        'CLERK',
        'LOAN_OFFICER',
        'ADMIN',
      ],
    },
    loadChildren: () =>
      import(
        './features/customers/customer-management.routes'
        ).then(
        (module) => module.customerManagementRoutes,
      ),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./features/access/pages/forbidden/forbidden')
        .then((module) => module.Forbidden),
  },
  {
    path: 'current-employee',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/employees/pages/current-employee/current-employee'
        ).then(
        (module) => module.CurrentEmployee,
      ),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
