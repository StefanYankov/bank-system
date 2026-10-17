import {Routes} from '@angular/router';

export const employeeManagementRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'employees',
  },
  {
    path: 'employees',
    loadComponent: () =>
      import('./pages/employee-list/employee-list')
        .then((module) => module.EmployeeList),
  },
];
