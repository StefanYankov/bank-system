import {Routes} from '@angular/router';
import {roleGuard, ROUTE_ROLES} from '../../core/auth/guards/role-guard';

const CUSTOMER_MAINTENANCE_ROLES = ['CLERK', 'ADMIN'];

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
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-registration/individual-customer-registration'
        ).then((module) => module.IndividualCustomerRegistration),
  },
  {
    path: 'individuals/:id/name',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-name-update/individual-customer-name-update'
        ).then((module) => module.IndividualCustomerNameUpdate),
  },
  {
    path: 'individuals/:id/contact',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-contact-update/individual-customer-contact-update'
        ).then((module) => module.IndividualCustomerContactUpdate),
  },
  {
    path: 'individuals/:id/residential-address',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-address-update/individual-customer-address-update'
        ).then((module) => module.IndividualCustomerAddressUpdate),
  },
  {
    path: 'individuals/:id/egn',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-egn-correction/individual-customer-egn-correction'
        ).then((module) => module.IndividualCustomerEgnCorrection),
  },
  {
    path: 'individuals/:id/close',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-close/individual-customer-close'
        ).then((module) => module.IndividualCustomerClose),
  },
  {
    path: 'individuals/:id/reopen',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-reopen/individual-customer-reopen'
        ).then((module) => module.IndividualCustomerReopen),
  },
  {
    path: 'individuals/:id',
    loadComponent: () =>
      import(
        './individual/pages/individual-customer-details/individual-customer-details'
        ).then((module) => module.IndividualCustomerDetails),
  },
  {
    path: 'legal-entities/register',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import(
        './legal-entity/pages/legal-entity-customer-registration/legal-entity-customer-registration'
        ).then((module) => module.LegalEntityCustomerRegistration),
  },
  {
    path: 'legal-entities/:id',
    loadComponent: () =>
      import(
        './legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details'
        ).then((module) => module.LegalEntityCustomerDetails),
  },
];
