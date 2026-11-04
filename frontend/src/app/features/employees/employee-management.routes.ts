import { Routes } from '@angular/router';

/**
 * Routes for ADMIN Employee operations.
 *
 * The route group is mounted below `/admin` by the root route configuration,
 * so all routes declared here inherit the parent ADMIN route guard.
 */
export const employeeManagementRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'employees',
  },
  {
    path: 'employees',
    loadComponent: () =>
      import('./pages/employee-list/employee-list').then((module) => module.EmployeeList),
  },
  {
    path: 'employees/new',
    loadComponent: () =>
      import('./pages/employee-registration/employee-registration').then(
        (module) => module.EmployeeRegistration,
      ),
  },
  {
    path: 'employees/:id/edit',
    loadComponent: () =>
      import('./pages/employee-profile-edit/employee-profile-edit').then(
        (module) => module.EmployeeProfileEdit,
      ),
  },
  {
    path: 'employees/:id/position',
    loadComponent: () =>
      import('./pages/employee-position-change/employee-position-change').then(
        (module) => module.EmployeePositionChange,
      ),
  },
  {
    path: 'employees/:id/disable',
    loadComponent: () =>
      import('./pages/employee-disable/employee-disable').then((module) => module.EmployeeDisable),
  },
  {
    path: 'employees/:id/reactivate',
    loadComponent: () =>
      import('./pages/employee-reactivate/employee-reactivate').then(
        (module) => module.EmployeeReactivate,
      ),
  },
  {
    path: 'employees/:id',
    loadComponent: () =>
      import('./pages/employee-details/employee-details').then((module) => module.EmployeeDetails),
  },
];
