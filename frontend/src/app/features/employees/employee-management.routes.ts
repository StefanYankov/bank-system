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
  {
    path: 'employees/:id',
    loadComponent: () =>
      import('./pages/employee-details/employee-details')
        .then((module) => module.EmployeeDetails),
  },
];
