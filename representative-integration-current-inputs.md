## frontend/src/app/features/customers/customer-management.routes.ts

```
import { Routes } from '@angular/router';
import { roleGuard, ROUTE_ROLES } from '../../core/auth/guards/role-guard';

const CUSTOMER_MAINTENANCE_ROLES = ['CLERK', 'ADMIN'];

export const customerManagementRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/customer-search/customer-search').then((module) => module.CustomerSearch),
  },
  {
    path: 'individuals/register',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-registration/individual-customer-registration').then(
        (module) => module.IndividualCustomerRegistration,
      ),
  },
  {
    path: 'individuals/:id/name',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-name-update/individual-customer-name-update').then(
        (module) => module.IndividualCustomerNameUpdate,
      ),
  },
  {
    path: 'individuals/:id/contact',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-contact-update/individual-customer-contact-update').then(
        (module) => module.IndividualCustomerContactUpdate,
      ),
  },
  {
    path: 'individuals/:id/residential-address',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-address-update/individual-customer-address-update').then(
        (module) => module.IndividualCustomerAddressUpdate,
      ),
  },
  {
    path: 'individuals/:id/egn',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-egn-correction/individual-customer-egn-correction').then(
        (module) => module.IndividualCustomerEgnCorrection,
      ),
  },
  {
    path: 'individuals/:id/close',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-close/individual-customer-close').then(
        (module) => module.IndividualCustomerClose,
      ),
  },
  {
    path: 'individuals/:id/reopen',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./individual/pages/individual-customer-reopen/individual-customer-reopen').then(
        (module) => module.IndividualCustomerReopen,
      ),
  },
  {
    path: 'individuals/:id',
    loadComponent: () =>
      import('./individual/pages/individual-customer-details/individual-customer-details').then(
        (module) => module.IndividualCustomerDetails,
      ),
  },
  {
    path: 'legal-entities/register',
    canActivate: [roleGuard],
    data: { [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-customer-registration/legal-entity-customer-registration').then(
        (module) => module.LegalEntityCustomerRegistration,
      ),
  },
  {
    path: 'legal-entities/:id/close',
    canActivate: [roleGuard],
    data: { [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-customer-close/legal-entity-customer-close').then(
        (module) => module.LegalEntityCustomerClose,
      ),
  },
  {
    path: 'legal-entities/:id/reopen',
    canActivate: [roleGuard],
    data: { [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-customer-reopen/legal-entity-customer-reopen').then(
        (module) => module.LegalEntityCustomerReopen,
      ),
  },
  {
    path: 'legal-entities/:id/company-name',
    canActivate: [roleGuard],
    data: { [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-company-name-update/legal-entity-company-name-update').then(
        (module) => module.LegalEntityCompanyNameUpdate,
      ),
  },
  {
    path: 'legal-entities/:id/contact',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-contact-update/legal-entity-contact-update').then(
        (module) => module.LegalEntityContactUpdate,
      ),
  },
  {
    path: 'legal-entities/:id/registered-address',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-registered-address-update/legal-entity-registered-address-update').then(
        (module) => module.LegalEntityRegisteredAddressUpdate,
      ),
  },
  {
    path: 'legal-entities/:id/correspondence-address',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-correspondence-address-update/legal-entity-correspondence-address-update').then(
        (module) => module.LegalEntityCorrespondenceAddressUpdate,
      ),
  },
  {
    path: 'legal-entities/:id/eik',
    canActivate: [roleGuard],
    data: {
      [ROUTE_ROLES]: CUSTOMER_MAINTENANCE_ROLES,
    },
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-eik-correction/legal-entity-eik-correction').then(
        (module) => module.LegalEntityEikCorrection,
      ),
  },
  {
    path: 'legal-entities/:id',
    loadComponent: () =>
      import('./legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details').then(
        (module) => module.LegalEntityCustomerDetails,
      ),
  },
];

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthSession } from '../../../../../core/auth/auth-session';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';

/**
 * Displays the details of one Legal Entity Customer.
 */
@Component({
  selector: 'app-legal-entity-customer-details',
  imports: [RouterLink],
  templateUrl: './legal-entity-customer-details.html',
  styleUrl: './legal-entity-customer-details.css',
})
export class LegalEntityCustomerDetails implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly today = localIsoDate(new Date());
  private readonly authSession = inject(AuthSession);
  protected readonly canMaintainCustomers =
    this.authSession.hasRealmRole('CLERK') || this.authSession.hasRealmRole('ADMIN');

  public ngOnInit(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      this.loading.set(false);
      this.error.set('The Customer identifier is missing.');
      return;
    }
    this.loadCustomer(customerId);
  }

  /**
   * Repeats the request for the Customer identified by the current route.
   */
  protected retry(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      return;
    }
    this.loadCustomer(customerId);
  }

  /**
   * Builds the displayed full name of a Representative.
   *
   * @param representative Representative to display
   * @returns first, optional middle, and last name separated by spaces
   */
  protected representativeName(representative: RepresentativeViewDto): string {
    return [representative.firstName, representative.middleName, representative.lastName]
      .filter((part): part is string => part !== null && part.length > 0)
      .join(' ');
  }

  /**
   * Converts a Representative role to its display label.
   *
   * @param role Representative role
   * @returns display label
   */
  protected roleLabel(role: RepresentativeViewDto.RoleEnum): string {
    return role === RepresentativeViewDto.RoleEnum.LegalRepresentative
      ? 'Legal representative'
      : 'Authorized representative';
  }

  /**
   * Reports whether a Representative's authority has ended.
   *
   * @param representative Representative to check
   * @returns true when the inclusive valid-to date is before today
   */
  protected hasEnded(representative: RepresentativeViewDto): boolean {
    return representative.validTo !== null && representative.validTo < this.today;
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    return customerId !== null && customerId.length > 0 ? customerId : null;
  }

  private loadCustomer(customerId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.customer.set(null);
    this.customerApi
      .getLegalEntityCustomer({ id: customerId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          this.customer.set(customer);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(this.errorMessage(error));
          this.loading.set(false);
        },
      });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Customer.';
      }
    }
    return 'The Legal Entity Customer could not be loaded. Please try again.';
  }
}

/**
 * Formats a date as a local calendar date without converting it to UTC.
 *
 * The returned format matches the ISO date-only values used by Customer
 * Service, allowing safe chronological comparison of `YYYY-MM-DD` strings.
 *
 * @param date date whose local calendar fields should be formatted
 * @returns local date in `YYYY-MM-DD` format
 */
function localIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details.html

```
<section aria-labelledby="legal-entity-customer-details-title" class="customer-details">
  <a class="back-link" routerLink="/customers"> ← Back to customers </a>
  @if (loading()) {
    <p class="state-message" role="status">Loading Legal Entity Customer...</p>
  } @else if (error(); as errorMessage) {
    <section class="state-panel state-panel--error" role="alert">
      <h1 id="legal-entity-customer-details-title">Customer unavailable</h1>
      <p>{{ errorMessage }}</p>
      <button (click)="retry()" class="button button--primary" type="button">Try again</button>
    </section>
  } @else if (customer(); as customerRecord) {
    <header class="customer-details__header">
      <div>
        <p class="customer-details__section-label">Legal Entity Customer</p>
        <p class="customer-details__field-label">Company name</p>
        <h1 id="legal-entity-customer-details-title">
          {{ customerRecord.companyName }}
        </h1>
        <p class="customer-details__field-label customer-details__number-label">Customer number</p>
        <p class="customer-details__number">
          {{ customerRecord.customerNumber }}
        </p>
      </div>
      <span [class.status-badge--closed]="customerRecord.status === 'CLOSED'" class="status-badge">
        {{ customerRecord.status }}
      </span>
    </header>
    @if (canMaintainCustomers) {
      <nav aria-label="Legal Entity Customer actions" class="customer-actions">
        @if (customerRecord.status === 'ACTIVE') {
          <a
            [routerLink]="['/customers/legal-entities', customerRecord.id, 'company-name']"
            class="button button--secondary"
            id="update-company-name-action"
          >
            Update company name
          </a>
          <a
            [routerLink]="['/customers/legal-entities', customerRecord.id, 'contact']"
            class="button button--secondary"
            id="update-contact-action"
          >
            Update contact
          </a>
          <a
            [routerLink]="['/customers/legal-entities', customerRecord.id, 'registered-address']"
            class="button button--secondary"
            id="update-registered-address-action"
          >
            Update registered address
          </a>
          <a
            [routerLink]="[
              '/customers/legal-entities',
              customerRecord.id,
              'correspondence-address',
            ]"
            class="button button--secondary"
            id="update-correspondence-address-action"
          >
            Update correspondence address
          </a>
          <a
            [routerLink]="['/customers/legal-entities', customerRecord.id, 'eik']"
            class="button button--secondary"
            id="correct-eik-action"
          >
            Correct EIK
          </a>
          <a
            [routerLink]="['/customers/legal-entities', customerRecord.id, 'close']"
            class="button button--secondary"
            id="close-action"
          >
            Close customer
          </a>
        } @else {
          <a
            [routerLink]="['/customers/legal-entities', customerRecord.id, 'reopen']"
            class="button button--secondary"
            id="reopen-action"
          >
            Reopen customer
          </a>
        }
      </nav>
    }
    <section aria-labelledby="company-details-title" class="details-card">
      <h2 id="company-details-title">Company and contact details</h2>
      <dl class="details-grid">
        <div class="details-grid__item">
          <dt>EIK</dt>
          <dd>{{ customerRecord.eik }}</dd>
        </div>
        <div class="details-grid__item">
          <dt>Email</dt>
          <dd>{{ customerRecord.email }}</dd>
        </div>
        <div class="details-grid__item">
          <dt>Phone</dt>
          <dd>{{ customerRecord.phone }}</dd>
        </div>
      </dl>
    </section>
    <section
      aria-labelledby="registered-address-title"
      class="details-card"
      id="registered-address"
    >
      <h2 id="registered-address-title">Registered address</h2>
      <address class="address">
        {{ customerRecord.registeredAddress.addressLine1 }}<br />
        @if (customerRecord.registeredAddress.addressLine2) {
          {{ customerRecord.registeredAddress.addressLine2 }}<br />
        }
        {{ customerRecord.registeredAddress.postalCode }}
        {{ customerRecord.registeredAddress.city }}<br />
        {{ customerRecord.registeredAddress.countryCode }}
      </address>
    </section>
    <section
      aria-labelledby="correspondence-address-title"
      class="details-card"
      id="correspondence-address"
    >
      <h2 id="correspondence-address-title">Correspondence address</h2>
      <address class="address">
        {{ customerRecord.correspondenceAddress.addressLine1 }}<br />
        @if (customerRecord.correspondenceAddress.addressLine2) {
          {{ customerRecord.correspondenceAddress.addressLine2 }}<br />
        }
        {{ customerRecord.correspondenceAddress.postalCode }}
        {{ customerRecord.correspondenceAddress.city }}<br />
        {{ customerRecord.correspondenceAddress.countryCode }}
      </address>
    </section>
    <section aria-labelledby="representatives-title" class="details-card">
      <h2 id="representatives-title">Representatives</h2>
      @if (customerRecord.representatives.length === 0) {
        <p class="representatives-empty">No representatives are recorded.</p>
      } @else {
        <div class="representatives-table-container">
          <table class="representatives-table">
            <caption class="visually-hidden">
              Representatives
            </caption>
            <thead>
              <tr>
                <th scope="col">Name</th>
                <th scope="col">Role</th>
                <th scope="col">Valid from</th>
                <th scope="col">Valid to</th>
                <th scope="col">Status</th>
                <th scope="col">Individual Customer</th>
              </tr>
            </thead>
            <tbody>
              @for (representative of customerRecord.representatives; track representative.id) {
                <tr>
                  <td>{{ representativeName(representative) }}</td>
                  <td>{{ roleLabel(representative.role) }}</td>
                  <td>{{ representative.validFrom }}</td>
                  <td>{{ representative.validTo ?? '—' }}</td>
                  <td>
                    <span
                      [class.representation-badge--ended]="hasEnded(representative)"
                      class="representation-badge"
                    >
                      {{ hasEnded(representative) ? 'Ended' : 'Current' }}
                    </span>
                  </td>
                  <td>
                    @if (representative.individualCustomerId; as individualCustomerId) {
                      <a
                        [routerLink]="['/customers/individuals', individualCustomerId]"
                        class="representative-link"
                      >
                        View customer
                      </a>
                    } @else {
                      Not linked
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  }
</section>

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details.css

```
:host {
  display: block;
}

.representatives-empty {
  margin: 1rem 0 0;
  color: #626262;
}

.representatives-table-container {
  margin-top: 1.25rem;
  overflow-x: auto;
}

.representatives-table {
  width: 100%;
  border-collapse: collapse;
}

.representatives-table th,
.representatives-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e4e4e4;
  text-align: left;
  white-space: nowrap;
}

.representatives-table th {
  color: #650016;
  background: #faf4f5;
  font-size: 0.8rem;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.representatives-table tbody tr:last-child td {
  border-bottom: 0;
}

.representation-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  color: #14532d;
  background: #dcfce7;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.representation-badge--ended {
  color: #4b5563;
  background: #e5e7eb;
}

.representative-link {
  color: #8f001f;
  font-weight: 700;
  text-decoration: none;
}

.representative-link:hover {
  text-decoration: underline;
}

.representative-link:focus-visible {
  outline: 0.2rem solid #d7193f;
  outline-offset: 0.2rem;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  border: 0;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details.spec.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthSession } from '../../../../../core/auth/auth-session';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityCustomerDetails } from './legal-entity-customer-details';

describe('LegalEntityCustomerDetails', () => {
  const customerId = '0199e65a-35b1-7000-8000-000000000102';
  const linkedIndividualId = '0199e65a-35b1-7000-8000-000000000101';
  const currentRepresentative: RepresentativeViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000201',
    individualCustomerId: linkedIndividualId,
    firstName: 'Serafim',
    middleName: 'Ivanov',
    lastName: 'Gerasimoff',
    role: RepresentativeViewDto.RoleEnum.LegalRepresentative,
    validFrom: '2020-01-01',
    validTo: null,
  };
  const endedRepresentative: RepresentativeViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000202',
    individualCustomerId: null,
    firstName: 'Jane',
    middleName: null,
    lastName: 'Doe',
    role: RepresentativeViewDto.RoleEnum.AuthorizedRepresentative,
    validFrom: '2020-01-01',
    validTo: '2021-12-31',
  };
  const customer: LegalEntityCustomerViewDto = {
    id: customerId,
    customerNumber: 'CUS-000002',
    status: LegalEntityCustomerViewDto.StatusEnum.Active,
    email: 'office@starter.example.com',
    phone: '+35921234567',
    companyName: 'Starter Ltd',
    eik: '123456786',
    registeredAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: 'Floor 3',
    },
    correspondenceAddress: {
      countryCode: 'BG',
      city: 'Plovdiv',
      postalCode: '4000',
      addressLine1: '10 Main Street',
      addressLine2: null,
    },
    representatives: [currentRepresentative, endedRepresentative],
  };
  const closedCustomer: LegalEntityCustomerViewDto = {
    ...customer,
    status: LegalEntityCustomerViewDto.StatusEnum.Closed,
  };
  const getLegalEntityCustomerMock = vi.fn();
  let realmRoles: readonly string[] = [];

  beforeEach(async () => {
    vi.clearAllMocks();
    realmRoles = ['CLERK'];
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    await TestBed.configureTestingModule({
      imports: [LegalEntityCustomerDetails],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: customerId }),
            },
          },
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
          },
        },
        {
          provide: AuthSession,
          useValue: {
            hasRealmRole: (role: string): boolean => realmRoles.includes(role),
          },
        },
      ],
    }).compileComponents();
  });

  it('should request and render the Legal Entity Customer details', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(getLegalEntityCustomerMock).toHaveBeenCalledOnce();
    expect(getLegalEntityCustomerMock).toHaveBeenCalledWith({ id: customerId });
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Starter Ltd');
    expect(element.textContent).toContain('CUS-000002');
    expect(element.textContent).toContain('123456786');
    expect(element.textContent).toContain('office@starter.example.com');
    expect(element.textContent).toContain('+35921234567');
    expect(element.textContent).toContain('ACTIVE');
  });

  it('should show maintenance and close actions to a clerk for an active Customer', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('#update-company-name-action')?.getAttribute('href')).toBe(
      `/customers/legal-entities/${customerId}/company-name`,
    );
    expect(element.querySelector('#update-contact-action')?.getAttribute('href')).toBe(
      `/customers/legal-entities/${customerId}/contact`,
    );
    expect(element.querySelector('#update-registered-address-action')?.getAttribute('href')).toBe(
      `/customers/legal-entities/${customerId}/registered-address`,
    );
    expect(
      element.querySelector('#update-correspondence-address-action')?.getAttribute('href'),
    ).toBe(`/customers/legal-entities/${customerId}/correspondence-address`);
    expect(element.querySelector('#correct-eik-action')?.getAttribute('href')).toBe(
      `/customers/legal-entities/${customerId}/eik`,
    );
    expect(element.querySelector('#close-action')?.getAttribute('href')).toBe(
      `/customers/legal-entities/${customerId}/close`,
    );
    expect(element.querySelector('#reopen-action')).toBeNull();
  });

  it('should show maintenance actions to an administrator', () => {
    realmRoles = ['ADMIN'];
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.customer-actions'),
    ).not.toBeNull();
  });

  it('should hide every action from a loan officer', () => {
    realmRoles = ['LOAN_OFFICER'];
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.customer-actions')).toBeNull();
    expect(element.textContent).toContain('Starter Ltd');
  });

  it('should show only reopening for a closed Customer', () => {
    getLegalEntityCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('#reopen-action')?.getAttribute('href')).toBe(
      `/customers/legal-entities/${customerId}/reopen`,
    );
    expect(element.querySelector('#update-company-name-action')).toBeNull();
    expect(element.querySelector('#close-action')).toBeNull();
  });

  it('should render both addresses', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const registered = element.querySelector('#registered-address')?.textContent ?? '';
    const correspondence = element.querySelector('#correspondence-address')?.textContent ?? '';
    expect(registered).toContain('1 Alexander Battenberg Square');
    expect(registered).toContain('Floor 3');
    expect(correspondence).toContain('10 Main Street');
    expect(correspondence).toContain('Plovdiv');
  });

  it('should render current and ended Representatives', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const rows = Array.from(element.querySelectorAll('.representatives-table tbody tr'));
    expect(rows).toHaveLength(2);
    expect(rows[0]?.textContent).toContain('Serafim Ivanov Gerasimoff');
    expect(rows[0]?.textContent).toContain('Legal representative');
    expect(rows[0]?.textContent).toContain('Current');
    expect(rows[1]?.textContent).toContain('Jane Doe');
    expect(rows[1]?.textContent).toContain('Authorized representative');
    expect(rows[1]?.textContent).toContain('Ended');
    expect(rows[1]?.textContent).toContain('Not linked');
  });

  it('should link a Representative to the linked Individual Customer', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const links = element.querySelectorAll<HTMLAnchorElement>('.representative-link');
    expect(links).toHaveLength(1);
    expect(links[0]?.getAttribute('href')).toBe(`/customers/individuals/${linkedIndividualId}`);
  });

  it('should show an empty message when no Representatives are recorded', () => {
    getLegalEntityCustomerMock.mockReturnValue(of({ ...customer, representatives: [] }));
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No representatives are recorded.');
    expect(element.querySelector('.representatives-table')).toBeNull();
  });

  it('should not display the technical Customer identifier as text', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).not.toContain(customerId);
  });

  it('should show a loading state while the request is pending', () => {
    getLegalEntityCustomerMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Loading Legal Entity Customer');
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show a not-found message for a missing Customer', () => {
    getLegalEntityCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain(
      'The requested Legal Entity Customer could not be found.',
    );
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show an authorization message for a forbidden response', () => {
    getLegalEntityCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'You do not have permission to view this Customer.',
    );
  });

  it('should retry loading the Legal Entity Customer', () => {
    getLegalEntityCustomerMock
      .mockReturnValueOnce(throwError(() => new Error('Customer Service unavailable')))
      .mockReturnValueOnce(of(customer));
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const retryButton = element.querySelector('.state-panel .button') as HTMLButtonElement | null;
    retryButton?.click();
    fixture.detectChanges();
    expect(getLegalEntityCustomerMock).toHaveBeenCalledTimes(2);
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Starter Ltd');
    expect(element.querySelector('.state-panel--error')).toBeNull();
  });
});

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-addition/legal-entity-representative-addition.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { notBlankValidator } from '../../../../../core/forms/not-blank-validator';
import {
  CustomerAPIService,
  CustomerSummaryDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeAdditionDto,
} from '../../../../../generated/customer-api';
import { customerSchemaConstraints } from '../../../../../generated/customer-schema-constraints.generated';

type RepresentativeMode = 'LINKED' | 'UNLINKED';
type NameControl = 'firstName' | 'middleName' | 'lastName';

const representativeConstraints = customerSchemaConstraints.RepresentativeAdditionDto;

/**
 * Adds a linked or unlinked Representative to an active Legal Entity Customer.
 */
@Component({
  selector: 'app-legal-entity-representative-addition',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-representative-addition.html',
  styleUrl: './legal-entity-representative-addition.css',
})
export class LegalEntityRepresentativeAddition implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly searchResults = signal<readonly CustomerSummaryDto[]>([]);
  protected readonly searching = signal(false);
  protected readonly searchError = signal<string | null>(null);
  protected readonly searchPerformed = signal(false);
  protected readonly selectedCustomer = signal<CustomerSummaryDto | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly customerApi = inject(CustomerAPIService);
  private readonly legalEntityCustomerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly representativeForm = this.formBuilder.group({
    mode: this.formBuilder.control<RepresentativeMode>('LINKED'),
    firstName: this.formBuilder.control('', [
      Validators.maxLength(representativeConstraints.firstName.maxLength),
    ]),
    middleName: this.formBuilder.control('', [
      Validators.maxLength(representativeConstraints.middleName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [
      Validators.maxLength(representativeConstraints.lastName.maxLength),
    ]),
    role: this.formBuilder.control<RepresentativeAdditionDto.RoleEnum>(
      RepresentativeAdditionDto.RoleEnum.LegalRepresentative,
      [Validators.required],
    ),
    validFrom: this.formBuilder.control('', [Validators.required]),
  });

  protected readonly searchForm = this.formBuilder.group({
    query: this.formBuilder.control('', [Validators.required, notBlankValidator()]),
  });

  public ngOnInit(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      this.loading.set(false);
      this.loadError.set('The Customer identifier is missing.');
      return;
    }

    this.legalEntityCustomerApi
      .getLegalEntityCustomer({ id: customerId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          this.customer.set(customer);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Returns whether the linked-Representative mode is selected.
   *
   * @returns true when the Representative will be linked to an Individual Customer
   */
  protected isLinkedMode(): boolean {
    return this.representativeForm.controls.mode.value === 'LINKED';
  }

  /**
   * Changes the Representative source and clears values belonging to the other mode.
   *
   * @param mode selected Representative source
   */
  protected changeMode(mode: RepresentativeMode): void {
    this.representativeForm.controls.mode.setValue(mode);
    this.submissionError.set(null);

    if (mode === 'LINKED') {
      this.clearNames();
      return;
    }

    this.selectedCustomer.set(null);
    this.searchResults.set([]);
    this.searchError.set(null);
    this.searchPerformed.set(false);
  }

  /**
   * Searches for active Individual Customers that can be linked as Representatives.
   */
  protected searchCustomers(): void {
    this.normalizeSearchQuery();
    if (this.searchForm.invalid || this.searching()) {
      this.searchForm.markAllAsTouched();
      return;
    }

    const query = this.searchForm.controls.query.value;
    this.searching.set(true);
    this.searchError.set(null);
    this.searchPerformed.set(true);
    this.searchResults.set([]);

    this.customerApi
      .searchCustomers({ query, page: 0, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const eligibleCustomers = page.content.filter((candidate) => {
            const isIndividual =
              candidate.customerType === CustomerSummaryDto.CustomerTypeEnum.Individual;
            const isActive = candidate.status === CustomerSummaryDto.StatusEnum.Active;
            return isIndividual && isActive;
          });

          this.searchResults.set(eligibleCustomers);
          this.searching.set(false);
        },
        error: () => {
          this.searchError.set('Individual Customers could not be searched. Please try again.');
          this.searching.set(false);
        },
      });
  }

  /**
   * Selects an active Individual Customer as the linked Representative.
   *
   * @param customer selected Individual Customer summary
   */
  protected selectCustomer(customer: CustomerSummaryDto): void {
    this.selectedCustomer.set(customer);
    this.searchResults.set([]);
    this.searchError.set(null);
  }

  /**
   * Clears the selected Individual Customer and returns to linked-Customer search.
   */
  protected clearSelectedCustomer(): void {
    this.selectedCustomer.set(null);
    this.searchPerformed.set(false);
  }

  /**
   * Reports whether a name field should display its validation message.
   *
   * @param controlName name control to inspect
   * @returns true when the control is invalid and touched
   */
  protected shouldShowNameError(controlName: NameControl): boolean {
    const control = this.representativeForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Adds the Representative using the selected source and form values.
   */
  protected submit(): void {
    const customer = this.customer();
    if (customer === null || this.submitting()) {
      return;
    }

    this.normalizeNames();
    this.applyModeValidation();

    if (!this.hasValidRepresentativeSource() || this.representativeForm.invalid) {
      this.representativeForm.markAllAsTouched();
      return;
    }

    const representativeAdditionDto = this.createRequest();
    this.submitting.set(true);
    this.submissionError.set(null);

    this.legalEntityCustomerApi
      .addRepresentative({ id: customer.id, representativeAdditionDto })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/customers/legal-entities', customer.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Returns route commands for the current Legal Entity Customer details page.
   *
   * @returns route commands for the Customer details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    return customerId !== null && customerId.length > 0 ? customerId : null;
  }

  private normalizeSearchQuery(): void {
    const query = this.searchForm.controls.query.value.trim();
    this.searchForm.controls.query.setValue(query, { emitEvent: false });
  }

  private normalizeNames(): void {
    const firstName = this.representativeForm.controls.firstName.value.trim();
    const middleName = this.representativeForm.controls.middleName.value.trim();
    const lastName = this.representativeForm.controls.lastName.value.trim();

    this.representativeForm.patchValue({ firstName, middleName, lastName }, { emitEvent: false });
  }

  private clearNames(): void {
    this.representativeForm.patchValue(
      { firstName: '', middleName: '', lastName: '' },
      { emitEvent: false },
    );
  }

  private applyModeValidation(): void {
    const firstName = this.representativeForm.controls.firstName;
    const lastName = this.representativeForm.controls.lastName;

    if (this.isLinkedMode()) {
      firstName.removeValidators([Validators.required, notBlankValidator()]);
      lastName.removeValidators([Validators.required, notBlankValidator()]);
    } else {
      firstName.addValidators([Validators.required, notBlankValidator()]);
      lastName.addValidators([Validators.required, notBlankValidator()]);
    }

    firstName.updateValueAndValidity({ emitEvent: false });
    lastName.updateValueAndValidity({ emitEvent: false });
  }

  private hasValidRepresentativeSource(): boolean {
    if (this.isLinkedMode()) {
      return this.selectedCustomer() !== null;
    }

    return true;
  }

  private createRequest(): RepresentativeAdditionDto {
    const formValue = this.representativeForm.getRawValue();
    const request: RepresentativeAdditionDto = {
      role: formValue.role,
      validFrom: formValue.validFrom,
    };

    if (formValue.mode === 'LINKED') {
      const selectedCustomer = this.selectedCustomer();
      if (selectedCustomer !== null) {
        request.individualCustomerId = selectedCustomer.id;
      }
      return request;
    }

    request.firstName = formValue.firstName;
    request.lastName = formValue.lastName;

    if (formValue.middleName.length > 0) {
      request.middleName = formValue.middleName;
    }

    return request;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Customer.';
      }
    }

    return 'The Legal Entity Customer could not be loaded. Please try again.';
  }

  private submissionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Representative information is invalid. Review the form and try again.';
      }
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The Legal Entity or linked Individual Customer could not be found.';
      }
      if (error.status === 409) {
        return 'The Representative could not be added because the Customer state changed.';
      }
    }

    return 'The Representative could not be added. Please try again.';
  }
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-addition/legal-entity-representative-addition.html

```
<section aria-labelledby="representative-addition-title" class="customer-maintenance">
  <a [routerLink]="customerLink()" class="back-link">← Back to customer</a>

  <header>
    <p class="customer-maintenance__label">Legal Entity Customer</p>
    <h1 id="representative-addition-title">Add Representative</h1>
    <p class="customer-maintenance__description">
      Link an existing Individual Customer or record an unlinked Representative.
    </p>
  </header>

  @if (loading()) {
    <p class="customer-maintenance__status" role="status">Loading Legal Entity Customer...</p>
  } @else if (loadError(); as errorMessage) {
    <section class="submission-error" role="alert">
      <h2>Customer unavailable</h2>
      <p>{{ errorMessage }}</p>
    </section>
  } @else if (customer(); as customerRecord) {
    @if (customerRecord.status === 'CLOSED') {
      <section class="submission-error" role="alert">
        <h2>Customer is closed</h2>
        <p>A closed Customer cannot be changed. Reopen the Customer first.</p>
      </section>
    } @else {
      @if (submissionError(); as errorMessage) {
        <section class="submission-error" role="alert">
          <h2>Addition unsuccessful</h2>
          <p>{{ errorMessage }}</p>
        </section>
      }

      <form
        (ngSubmit)="submit()"
        [formGroup]="representativeForm"
        class="registration-form"
        novalidate
      >
        <section aria-labelledby="source-title" class="form-section">
          <h2 id="source-title">Representative source</h2>
          <p class="form-section__description">
            A linked Representative uses the Individual Customer's recorded name. An unlinked
            Representative keeps the names entered here.
          </p>

          <div class="representative-mode" role="radiogroup" aria-label="Representative source">
            <label>
              <input
                (change)="changeMode('LINKED')"
                [checked]="isLinkedMode()"
                name="representative-mode"
                type="radio"
                value="LINKED"
              />
              Existing Individual Customer
            </label>
            <label>
              <input
                (change)="changeMode('UNLINKED')"
                [checked]="!isLinkedMode()"
                name="representative-mode"
                type="radio"
                value="UNLINKED"
              />
              Unlinked Representative
            </label>
          </div>

          @if (isLinkedMode()) {
            @if (selectedCustomer(); as selected) {
              <section class="selected-customer" aria-labelledby="selected-customer-title">
                <div>
                  <h3 id="selected-customer-title">Selected Individual Customer</h3>
                  <p>{{ selected.customerNumber }} · {{ selected.displayName }}</p>
                  <p class="field-help">EGN {{ selected.identifier }}</p>
                </div>
                <button
                  (click)="clearSelectedCustomer()"
                  class="button button--secondary"
                  type="button"
                >
                  Change selection
                </button>
              </section>
            } @else {
              <div [formGroup]="searchForm" class="representative-search">
                <div class="form-field form-field--full-width">
                  <label for="customer-query">Find an Individual Customer</label>
                  <div class="search-row">
                    <input
                      aria-describedby="customer-query-help customer-query-error"
                      formControlName="query"
                      id="customer-query"
                      type="search"
                    />
                    <button
                      (click)="searchCustomers()"
                      [disabled]="searching()"
                      class="button button--secondary"
                      type="button"
                    >
                      {{ searching() ? 'Searching...' : 'Search' }}
                    </button>
                  </div>
                  <p class="field-help" id="customer-query-help">
                    Search by Customer number, name, or EGN.
                  </p>
                  @if (searchForm.controls.query.invalid && searchForm.controls.query.touched) {
                    <p class="field-error" id="customer-query-error">Enter a search value.</p>
                  }
                </div>
              </div>

              @if (searchError(); as errorMessage) {
                <p class="representative-search__error" role="alert">{{ errorMessage }}</p>
              }

              @if (searchResults().length > 0) {
                <ul class="representative-results" aria-label="Eligible Individual Customers">
                  @for (candidate of searchResults(); track candidate.id) {
                    <li>
                      <div>
                        <strong
                          >{{ candidate.customerNumber }} · {{ candidate.displayName }}</strong
                        >
                        <span>EGN {{ candidate.identifier }}</span>
                      </div>
                      <button
                        (click)="selectCustomer(candidate)"
                        class="button button--secondary"
                        type="button"
                      >
                        Select
                      </button>
                    </li>
                  }
                </ul>
              } @else if (searchPerformed() && !searching() && !searchError()) {
                <p class="representative-results__empty">
                  No active Individual Customers matched the search.
                </p>
              }

              @if (representativeForm.touched && selectedCustomer() === null) {
                <p class="field-error">Select an active Individual Customer.</p>
              }
            }
          } @else {
            <div class="form-grid">
              <div class="form-field">
                <label for="first-name">First name</label>
                <input id="first-name" formControlName="firstName" type="text" />
                @if (shouldShowNameError('firstName')) {
                  <p class="field-error">
                    First name is required and must not exceed 100 characters.
                  </p>
                }
              </div>
              <div class="form-field">
                <label for="middle-name">Middle name</label>
                <input id="middle-name" formControlName="middleName" type="text" />
                @if (shouldShowNameError('middleName')) {
                  <p class="field-error">Middle name must not exceed 100 characters.</p>
                }
              </div>
              <div class="form-field">
                <label for="last-name">Last name</label>
                <input id="last-name" formControlName="lastName" type="text" />
                @if (shouldShowNameError('lastName')) {
                  <p class="field-error">
                    Last name is required and must not exceed 100 characters.
                  </p>
                }
              </div>
            </div>
          }
        </section>

        <section aria-labelledby="authority-title" class="form-section">
          <h2 id="authority-title">Authority</h2>
          <div class="form-grid">
            <div class="form-field">
              <label for="role">Role</label>
              <select id="role" formControlName="role">
                <option value="LEGAL_REPRESENTATIVE">Legal representative</option>
                <option value="AUTHORIZED_REPRESENTATIVE">Authorized representative</option>
              </select>
            </div>
            <div class="form-field">
              <label for="valid-from">Valid from</label>
              <input id="valid-from" formControlName="validFrom" type="date" />
              @if (
                representativeForm.controls.validFrom.invalid &&
                representativeForm.controls.validFrom.touched
              ) {
                <p class="field-error">Valid-from date is required.</p>
              }
            </div>
          </div>
        </section>

        <footer class="form-actions">
          <a [routerLink]="customerLink()" class="button button--secondary">Cancel</a>
          <button [disabled]="submitting()" class="button button--primary" type="submit">
            {{ submitting() ? 'Adding...' : 'Add Representative' }}
          </button>
        </footer>
      </form>
    }
  }
</section>

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-addition/legal-entity-representative-addition.css

```
:host {
  display: block;
}

.representative-mode {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem 2rem;
  margin-top: 1.25rem;
}

.representative-mode label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
}

.representative-search {
  margin-top: 1.25rem;
}

.search-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.75rem;
}

.selected-customer,
.representative-results li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.selected-customer {
  margin-top: 1.25rem;
  padding: 1rem;
  background: #faf4f5;
  border: 1px solid #d7b7be;
  border-radius: 0.4rem;
}

.selected-customer h3,
.selected-customer p {
  margin: 0;
}

.selected-customer p {
  margin-top: 0.35rem;
}

.representative-results {
  display: grid;
  gap: 0.75rem;
  margin: 1rem 0 0;
  padding: 0;
  list-style: none;
}

.representative-results li {
  padding: 1rem;
  border: 1px solid #dddddd;
  border-radius: 0.4rem;
}

.representative-results span {
  display: block;
  margin-top: 0.3rem;
  color: #626262;
  font-size: 0.85rem;
}

.representative-search__error,
.representative-results__empty {
  margin: 1rem 0 0;
  color: #626262;
}

.representative-search__error {
  color: #b91c1c;
  font-weight: 600;
}

@media (max-width: 40rem) {
  .search-row,
  .selected-customer,
  .representative-results li {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .selected-customer,
  .representative-results li {
    flex-direction: column;
  }
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-addition/legal-entity-representative-addition.spec.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  CustomerAPIService,
  CustomerSummaryDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityRepresentativeAddition } from './legal-entity-representative-addition';

describe('LegalEntityRepresentativeAddition', () => {
  const customerId = '0199e65a-35b1-7000-8000-000000000102';
  const individualCustomerId = '0199e65a-35b1-7000-8000-000000000101';
  const legalEntity: LegalEntityCustomerViewDto = {
    id: customerId,
    customerNumber: 'CUS-000002',
    status: LegalEntityCustomerViewDto.StatusEnum.Active,
    email: 'office@starter.example.com',
    phone: '+35921234567',
    companyName: 'Starter Ltd',
    eik: '123456786',
    registeredAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
    correspondenceAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
    representatives: [],
  };
  const individual: CustomerSummaryDto = {
    id: individualCustomerId,
    customerNumber: 'CUS-000001',
    customerType: CustomerSummaryDto.CustomerTypeEnum.Individual,
    displayName: 'Serafim Gerasimoff',
    identifier: '6101047500',
    status: CustomerSummaryDto.StatusEnum.Active,
  };
  const representative: RepresentativeViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000201',
    individualCustomerId,
    firstName: 'Serafim',
    middleName: null,
    lastName: 'Gerasimoff',
    role: RepresentativeViewDto.RoleEnum.LegalRepresentative,
    validFrom: '2026-11-04',
    validTo: null,
  };
  const getLegalEntityCustomerMock = vi.fn();
  const searchCustomersMock = vi.fn();
  const addRepresentativeMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(legalEntity));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityRepresentativeAddition],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: customerId }),
            },
          },
        },
        {
          provide: CustomerAPIService,
          useValue: {
            searchCustomers: searchCustomersMock,
          },
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
            addRepresentative: addRepresentativeMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Legal Entity Customer', () => {
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();

    expect(getLegalEntityCustomerMock).toHaveBeenCalledWith({ id: customerId });
    expect(fixture.nativeElement.textContent).toContain('Add Representative');
  });

  it('should keep only active Individual Customers in search results', () => {
    const legalEntitySummary: CustomerSummaryDto = {
      ...individual,
      id: customerId,
      customerType: CustomerSummaryDto.CustomerTypeEnum.LegalEntity,
      displayName: 'Starter Ltd',
      identifier: '123456786',
    };
    const closedIndividual: CustomerSummaryDto = {
      ...individual,
      id: '0199e65a-35b1-7000-8000-000000000103',
      status: CustomerSummaryDto.StatusEnum.Closed,
    };
    searchCustomersMock.mockReturnValue(
      of({
        content: [individual, legalEntitySummary, closedIndividual],
        page: 0,
        size: 10,
        totalElements: 3,
        totalPages: 1,
        last: true,
      }),
    );
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['searchForm'].controls.query.setValue('Serafim');

    component['searchCustomers']();
    fixture.detectChanges();

    expect(searchCustomersMock).toHaveBeenCalledWith({ query: 'Serafim', page: 0, size: 10 });
    expect(component['searchResults']()).toEqual([individual]);
  });

  it('should add a linked Representative without sending names', () => {
    addRepresentativeMock.mockReturnValue(of(representative));
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['selectCustomer'](individual);
    component['representativeForm'].patchValue({
      role: 'LEGAL_REPRESENTATIVE',
      validFrom: '2026-11-04',
    });

    component['submit']();

    expect(addRepresentativeMock).toHaveBeenCalledWith({
      id: customerId,
      representativeAdditionDto: {
        individualCustomerId,
        role: 'LEGAL_REPRESENTATIVE',
        validFrom: '2026-11-04',
      },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/legal-entities', customerId]);
  });

  it('should add an unlinked Representative with normalized names', () => {
    addRepresentativeMock.mockReturnValue(of({ ...representative, individualCustomerId: null }));
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['changeMode']('UNLINKED');
    component['representativeForm'].patchValue({
      firstName: '  Jane  ',
      middleName: '   ',
      lastName: '  Doe  ',
      role: 'AUTHORIZED_REPRESENTATIVE',
      validFrom: '2026-11-04',
    });

    component['submit']();

    expect(addRepresentativeMock).toHaveBeenCalledWith({
      id: customerId,
      representativeAdditionDto: {
        firstName: 'Jane',
        lastName: 'Doe',
        role: 'AUTHORIZED_REPRESENTATIVE',
        validFrom: '2026-11-04',
      },
    });
  });

  it('should require an Individual Customer in linked mode', () => {
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['representativeForm'].controls.validFrom.setValue('2026-11-04');

    component['submit']();

    expect(addRepresentativeMock).not.toHaveBeenCalled();
  });

  it('should require first and last names in unlinked mode', () => {
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['changeMode']('UNLINKED');
    component['representativeForm'].controls.validFrom.setValue('2026-11-04');

    component['submit']();

    expect(addRepresentativeMock).not.toHaveBeenCalled();
    expect(component['representativeForm'].controls.firstName.invalid).toBe(true);
    expect(component['representativeForm'].controls.lastName.invalid).toBe(true);
  });

  it('should not show the form for a closed Legal Entity Customer', () => {
    getLegalEntityCustomerMock.mockReturnValue(
      of({ ...legalEntity, status: LegalEntityCustomerViewDto.StatusEnum.Closed }),
    );
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Customer is closed');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('should prevent repeat submission while pending', () => {
    addRepresentativeMock.mockReturnValue(new Subject<RepresentativeViewDto>());
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['selectCustomer'](individual);
    component['representativeForm'].controls.validFrom.setValue('2026-11-04');

    component['submit']();
    component['submit']();

    expect(addRepresentativeMock).toHaveBeenCalledOnce();
  });

  it('should show a conflict without navigating away', () => {
    addRepresentativeMock.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['selectCustomer'](individual);
    component['representativeForm'].controls.validFrom.setValue('2026-11-04');

    component['submit']();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'The Representative could not be added because the Customer state changed.',
    );
    expect(navigateMock).not.toHaveBeenCalled();
  });
});

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-correction/legal-entity-representative-correction.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { notBlankValidator } from '../../../../../core/forms/not-blank-validator';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeCorrectionDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import { customerSchemaConstraints } from '../../../../../generated/customer-schema-constraints.generated';

type NameControl = 'firstName' | 'middleName' | 'lastName';

const constraints = customerSchemaConstraints.RepresentativeCorrectionDto;

/**
 * Corrects the recorded data of a Legal Entity Customer Representative.
 *
 * Linked Representatives keep their Individual Customer link and recorded name
 * snapshot. Unlinked Representative names remain editable. Role and validity
 * dates can be corrected for both types, including removing an incorrect end date.
 */
@Component({
  selector: 'app-legal-entity-representative-correction',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-representative-correction.html',
  styleUrl: './legal-entity-representative-correction.css',
})
export class LegalEntityRepresentativeCorrection implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly representative = signal<RepresentativeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly correctionForm = this.formBuilder.group({
    firstName: this.formBuilder.control('', [
      Validators.maxLength(constraints.firstName.maxLength),
    ]),
    middleName: this.formBuilder.control('', [
      Validators.maxLength(constraints.middleName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [Validators.maxLength(constraints.lastName.maxLength)]),
    role: this.formBuilder.control<RepresentativeCorrectionDto.RoleEnum>(
      RepresentativeCorrectionDto.RoleEnum.LegalRepresentative,
      [Validators.required],
    ),
    validFrom: this.formBuilder.control('', [Validators.required]),
    validTo: this.formBuilder.control(''),
  });

  public ngOnInit(): void {
    const customerId = this.customerId();
    const representativeId = this.representativeId();

    if (customerId === null || representativeId === null) {
      this.loading.set(false);
      this.loadError.set('The Customer or Representative identifier is missing.');
      return;
    }

    forkJoin({
      customer: this.customerApi.getLegalEntityCustomer({ id: customerId }),
      representative: this.customerApi.getRepresentative({ id: customerId, representativeId }),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ customer, representative }) => {
          this.customer.set(customer);
          this.representative.set(representative);
          this.populateForm(representative);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Reports whether the Representative is linked to an Individual Customer.
   *
   * @returns true when an Individual Customer link is recorded
   */
  protected isLinkedRepresentative(): boolean {
    return this.representative()?.individualCustomerId !== null;
  }

  /**
   * Reports whether a name field should display its validation message.
   *
   * @param controlName name control to inspect
   * @returns true when the control is invalid and touched
   */
  protected shouldShowNameError(controlName: NameControl): boolean {
    const control = this.correctionForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Corrects all recorded Representative values and returns to Customer details.
   */
  protected submit(): void {
    const customer = this.customer();
    const representative = this.representative();

    if (customer === null || representative === null || this.submitting()) {
      return;
    }

    this.normalizeNames();
    this.applyNameValidation(representative);

    if (this.correctionForm.invalid) {
      this.correctionForm.markAllAsTouched();
      return;
    }

    const representativeCorrectionDto = this.createRequest(representative);
    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .correctRepresentative({
        id: customer.id,
        representativeId: representative.id,
        representativeCorrectionDto,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/customers/legal-entities', customer.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Returns route commands for the Legal Entity Customer details page.
   *
   * @returns route commands for the Customer details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    if (customerId === null || customerId.length === 0) {
      return null;
    }
    return customerId;
  }

  private representativeId(): string | null {
    const representativeId = this.route.snapshot.paramMap.get('representativeId');
    if (representativeId === null || representativeId.length === 0) {
      return null;
    }
    return representativeId;
  }

  private populateForm(representative: RepresentativeViewDto): void {
    this.correctionForm.setValue({
      firstName: representative.firstName,
      middleName: representative.middleName ?? '',
      lastName: representative.lastName,
      role: representative.role,
      validFrom: representative.validFrom,
      validTo: representative.validTo ?? '',
    });
  }

  private normalizeNames(): void {
    const firstName = this.correctionForm.controls.firstName.value.trim();
    const middleName = this.correctionForm.controls.middleName.value.trim();
    const lastName = this.correctionForm.controls.lastName.value.trim();

    this.correctionForm.patchValue({ firstName, middleName, lastName }, { emitEvent: false });
  }

  private applyNameValidation(representative: RepresentativeViewDto): void {
    const firstName = this.correctionForm.controls.firstName;
    const lastName = this.correctionForm.controls.lastName;

    if (representative.individualCustomerId !== null) {
      firstName.removeValidators([Validators.required, notBlankValidator()]);
      lastName.removeValidators([Validators.required, notBlankValidator()]);
    } else {
      firstName.addValidators([Validators.required, notBlankValidator()]);
      lastName.addValidators([Validators.required, notBlankValidator()]);
    }

    firstName.updateValueAndValidity({ emitEvent: false });
    lastName.updateValueAndValidity({ emitEvent: false });
  }

  private createRequest(representative: RepresentativeViewDto): RepresentativeCorrectionDto {
    const value = this.correctionForm.getRawValue();
    const request: RepresentativeCorrectionDto = {
      role: value.role,
      validFrom: value.validFrom,
    };

    if (representative.individualCustomerId !== null) {
      request.individualCustomerId = representative.individualCustomerId;
    } else {
      request.firstName = value.firstName;
      request.lastName = value.lastName;

      if (value.middleName.length > 0) {
        request.middleName = value.middleName;
      }
    }

    if (value.validTo.length > 0) {
      request.validTo = value.validTo;
    }

    return request;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Representative.';
      }
    }

    return 'The Representative could not be loaded. Please try again.';
  }

  private submissionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Representative information is invalid. Review the form and try again.';
      }
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 409) {
        return 'The Representative could not be corrected because the Customer state changed.';
      }
    }

    return 'The Representative could not be corrected. Please try again.';
  }
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-correction/legal-entity-representative-correction.html

```
<section aria-labelledby="representative-correction-title" class="customer-maintenance">
  <a [routerLink]="customerLink()" class="back-link">← Back to customer</a>

  <header>
    <p class="customer-maintenance__label">Legal Entity Customer</p>
    <h1 id="representative-correction-title">Correct Representative</h1>
    <p class="customer-maintenance__description">
      Correct recorded Representative data. Clearing the valid-to date reopens a representation
      ended by mistake.
    </p>
  </header>

  @if (loading()) {
    <p class="customer-maintenance__status" role="status">Loading Representative...</p>
  } @else if (loadError(); as errorMessage) {
    <section class="submission-error" role="alert">
      <h2>Representative unavailable</h2>
      <p>{{ errorMessage }}</p>
    </section>
  } @else if (customer(); as customerRecord) {
    @if (customerRecord.status === 'CLOSED') {
      <section class="submission-error" role="alert">
        <h2>Customer is closed</h2>
        <p>A closed Customer cannot be changed. Reopen the Customer first.</p>
      </section>
    } @else if (representative(); as representativeRecord) {
      @if (submissionError(); as errorMessage) {
        <section class="submission-error" role="alert">
          <h2>Correction unsuccessful</h2>
          <p>{{ errorMessage }}</p>
        </section>
      }

      <form (ngSubmit)="submit()" [formGroup]="correctionForm" class="registration-form" novalidate>
        <section aria-labelledby="identity-title" class="form-section">
          <h2 id="identity-title">Representative identity</h2>

          @if (isLinkedRepresentative()) {
            <p class="form-section__description">
              This Representative is linked to an Individual Customer. The recorded name snapshot
              remains read-only.
            </p>
            <dl class="representative-summary">
              <div>
                <dt>Name</dt>
                <dd>
                  {{ representativeRecord.firstName }}
                  @if (representativeRecord.middleName) {
                    {{ representativeRecord.middleName }}
                  }
                  {{ representativeRecord.lastName }}
                </dd>
              </div>
              <div>
                <dt>Individual Customer</dt>
                <dd>
                  <a
                    [routerLink]="[
                      '/customers/individuals',
                      representativeRecord.individualCustomerId,
                    ]"
                  >
                    View linked Customer
                  </a>
                </dd>
              </div>
            </dl>
          } @else {
            <div class="form-grid">
              <div class="form-field">
                <label for="first-name">First name</label>
                <input id="first-name" formControlName="firstName" type="text" />
                @if (shouldShowNameError('firstName')) {
                  <p class="field-error">
                    First name is required and must not exceed 100 characters.
                  </p>
                }
              </div>
              <div class="form-field">
                <label for="middle-name">Middle name</label>
                <input id="middle-name" formControlName="middleName" type="text" />
                @if (shouldShowNameError('middleName')) {
                  <p class="field-error">Middle name must not exceed 100 characters.</p>
                }
              </div>
              <div class="form-field">
                <label for="last-name">Last name</label>
                <input id="last-name" formControlName="lastName" type="text" />
                @if (shouldShowNameError('lastName')) {
                  <p class="field-error">
                    Last name is required and must not exceed 100 characters.
                  </p>
                }
              </div>
            </div>
          }
        </section>

        <section aria-labelledby="authority-title" class="form-section">
          <h2 id="authority-title">Authority</h2>
          <div class="form-grid">
            <div class="form-field">
              <label for="role">Role</label>
              <select id="role" formControlName="role">
                <option value="LEGAL_REPRESENTATIVE">Legal representative</option>
                <option value="AUTHORIZED_REPRESENTATIVE">Authorized representative</option>
              </select>
            </div>
            <div class="form-field">
              <label for="valid-from">Valid from</label>
              <input id="valid-from" formControlName="validFrom" type="date" />
            </div>
            <div class="form-field">
              <label for="valid-to">Valid to</label>
              <input id="valid-to" formControlName="validTo" type="date" />
              <p class="field-help">Leave blank when the representation is current.</p>
            </div>
          </div>
        </section>

        <footer class="form-actions">
          <a [routerLink]="customerLink()" class="button button--secondary">Cancel</a>
          <button [disabled]="submitting()" class="button button--primary" type="submit">
            {{ submitting() ? 'Correcting...' : 'Correct Representative' }}
          </button>
        </footer>
      </form>
    }
  }
</section>

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-correction/legal-entity-representative-correction.css

```
:host {
  display: block;
}

.representative-summary {
  display: grid;
  gap: 1rem;
  margin: 1.25rem 0 0;
}

.representative-summary div {
  padding: 1rem;
  background: #faf4f5;
  border: 1px solid #d7b7be;
  border-radius: 0.4rem;
}

.representative-summary dt {
  color: #650016;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.representative-summary dd {
  margin: 0.35rem 0 0;
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representative-correction/legal-entity-representative-correction.spec.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityRepresentativeCorrection } from './legal-entity-representative-correction';

describe('LegalEntityRepresentativeCorrection', () => {
  const customerId = '0199e65a-35b1-7000-8000-000000000102';
  const representativeId = '0199e65a-35b1-7000-8000-000000000201';
  const individualCustomerId = '0199e65a-35b1-7000-8000-000000000101';
  const representative: RepresentativeViewDto = {
    id: representativeId,
    individualCustomerId,
    firstName: 'Serafim',
    middleName: 'Ivanov',
    lastName: 'Gerasimoff',
    role: RepresentativeViewDto.RoleEnum.LegalRepresentative,
    validFrom: '2026-01-01',
    validTo: null,
  };
  const customer: LegalEntityCustomerViewDto = {
    id: customerId,
    customerNumber: 'CUS-000002',
    status: LegalEntityCustomerViewDto.StatusEnum.Active,
    email: 'office@starter.example.com',
    phone: '+35921234567',
    companyName: 'Starter Ltd',
    eik: '123456786',
    registeredAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
    correspondenceAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
    representatives: [representative],
  };
  const getLegalEntityCustomerMock = vi.fn();
  const getRepresentativeMock = vi.fn();
  const correctRepresentativeMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    getRepresentativeMock.mockReturnValue(of(representative));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityRepresentativeCorrection],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: customerId, representativeId }),
            },
          },
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
            getRepresentative: getRepresentativeMock,
            correctRepresentative: correctRepresentativeMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and Representative', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityRepresentativeCorrection);

    // Act
    fixture.detectChanges();

    // Assert
    expect(getLegalEntityCustomerMock).toHaveBeenCalledWith({ id: customerId });
    expect(getRepresentativeMock).toHaveBeenCalledWith({ id: customerId, representativeId });
    expect(fixture.componentInstance['correctionForm'].controls.validFrom.value).toBe('2026-01-01');
  });

  it('should preserve a linked Customer and omit names from the request', () => {
    // Arrange
    correctRepresentativeMock.mockReturnValue(of(representative));
    const fixture = TestBed.createComponent(LegalEntityRepresentativeCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['correctionForm'].patchValue({
      role: 'AUTHORIZED_REPRESENTATIVE',
      validFrom: '2026-02-01',
      validTo: '',
    });

    // Act
    component['submit']();

    // Assert
    expect(correctRepresentativeMock).toHaveBeenCalledWith({
      id: customerId,
      representativeId,
      representativeCorrectionDto: {
        individualCustomerId,
        role: 'AUTHORIZED_REPRESENTATIVE',
        validFrom: '2026-02-01',
      },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/legal-entities', customerId]);
  });

  it('should normalize names for an unlinked Representative', () => {
    // Arrange
    const unlinked = { ...representative, individualCustomerId: null };
    getRepresentativeMock.mockReturnValue(of(unlinked));
    correctRepresentativeMock.mockReturnValue(of(unlinked));
    const fixture = TestBed.createComponent(LegalEntityRepresentativeCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['correctionForm'].patchValue({
      firstName: '  Jane  ',
      middleName: '   ',
      lastName: '  Doe  ',
      role: 'AUTHORIZED_REPRESENTATIVE',
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
    });

    // Act
    component['submit']();

    // Assert
    expect(correctRepresentativeMock).toHaveBeenCalledWith({
      id: customerId,
      representativeId,
      representativeCorrectionDto: {
        firstName: 'Jane',
        lastName: 'Doe',
        role: 'AUTHORIZED_REPRESENTATIVE',
        validFrom: '2026-01-01',
        validTo: '2026-12-31',
      },
    });
  });

  it('should not show the form for a closed Customer', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(
      of({ ...customer, status: LegalEntityCustomerViewDto.StatusEnum.Closed }),
    );
    const fixture = TestBed.createComponent(LegalEntityRepresentativeCorrection);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Customer is closed');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('should prevent repeat submission while pending', () => {
    // Arrange
    correctRepresentativeMock.mockReturnValue(new Subject<RepresentativeViewDto>());
    const fixture = TestBed.createComponent(LegalEntityRepresentativeCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(correctRepresentativeMock).toHaveBeenCalledOnce();
  });

  it('should show a conflict without navigating away', () => {
    // Arrange
    correctRepresentativeMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409 })),
    );
    const fixture = TestBed.createComponent(LegalEntityRepresentativeCorrection);
    fixture.detectChanges();

    // Act
    fixture.componentInstance['submit']();
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain(
      'The Representative could not be corrected because the Customer state changed.',
    );
    expect(navigateMock).not.toHaveBeenCalled();
  });
});

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representation-end/legal-entity-representation-end.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentationEndDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';

/**
 * Confirms and ends a current Legal Entity Customer representation.
 *
 * The final valid date is inclusive and cannot precede the first valid date.
 * Ending preserves the Representative record and Customer link for history.
 */
@Component({
  selector: 'app-legal-entity-representation-end',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-representation-end.html',
  styleUrl: './legal-entity-representation-end.css',
})
export class LegalEntityRepresentationEnd implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly representative = signal<RepresentativeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly dateError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly endForm = this.formBuilder.group({
    validTo: this.formBuilder.control('', [Validators.required]),
  });

  public ngOnInit(): void {
    const customerId = this.customerId();
    const representativeId = this.representativeId();

    if (customerId === null || representativeId === null) {
      this.loading.set(false);
      this.loadError.set('The Customer or Representative identifier is missing.');
      return;
    }

    forkJoin({
      customer: this.customerApi.getLegalEntityCustomer({ id: customerId }),
      representative: this.customerApi.getRepresentative({ id: customerId, representativeId }),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ customer, representative }) => {
          this.customer.set(customer);
          this.representative.set(representative);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Builds the displayed full name of the Representative being ended.
   *
   * @returns recorded Representative name
   */
  protected representativeName(): string {
    const representative = this.representative();
    if (representative === null) {
      return '';
    }

    const parts = [representative.firstName, representative.middleName, representative.lastName];
    return parts.filter((part): part is string => part !== null && part.length > 0).join(' ');
  }

  /**
   * Ends the representation after validating the inclusive final date.
   */
  protected confirm(): void {
    const customer = this.customer();
    const representative = this.representative();

    if (customer === null || representative === null || this.submitting()) {
      return;
    }

    this.dateError.set(null);

    if (this.endForm.invalid) {
      this.endForm.markAllAsTouched();
      return;
    }

    const validTo = this.endForm.controls.validTo.value;
    if (validTo < representative.validFrom) {
      this.dateError.set('Valid-to date cannot be before the valid-from date.');
      return;
    }

    const representationEndDto: RepresentationEndDto = { validTo };
    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .endRepresentation({
        id: customer.id,
        representativeId: representative.id,
        representationEndDto,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/customers/legal-entities', customer.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Returns route commands for the Legal Entity Customer details page.
   *
   * @returns route commands for the Customer details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    if (customerId === null || customerId.length === 0) {
      return null;
    }
    return customerId;
  }

  private representativeId(): string | null {
    const representativeId = this.route.snapshot.paramMap.get('representativeId');
    if (representativeId === null || representativeId.length === 0) {
      return null;
    }
    return representativeId;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Representative.';
      }
    }

    return 'The Representative could not be loaded. Please try again.';
  }

  private submissionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The final valid date is invalid. Review the date and try again.';
      }
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 409) {
        return 'The representation is already ended or the Customer state changed.';
      }
    }

    return 'The representation could not be ended. Please try again.';
  }
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representation-end/legal-entity-representation-end.html

```
<section aria-labelledby="representation-end-title" class="customer-maintenance">
  <a [routerLink]="customerLink()" class="back-link">← Back to customer</a>

  <header>
    <p class="customer-maintenance__label">Legal Entity Customer</p>
    <h1 id="representation-end-title">End Representation</h1>
    <p class="customer-maintenance__description">
      Record the final inclusive date of the Representative's authority.
    </p>
  </header>

  @if (loading()) {
    <p class="customer-maintenance__status" role="status">Loading Representative...</p>
  } @else if (loadError(); as errorMessage) {
    <section class="submission-error" role="alert">
      <h2>Representative unavailable</h2>
      <p>{{ errorMessage }}</p>
    </section>
  } @else if (customer(); as customerRecord) {
    @if (customerRecord.status === 'CLOSED') {
      <section class="submission-error" role="alert">
        <h2>Customer is closed</h2>
        <p>A closed Customer cannot be changed. Reopen the Customer first.</p>
      </section>
    } @else if (representative(); as representativeRecord) {
      @if (representativeRecord.validTo !== null) {
        <section class="submission-error" role="alert">
          <h2>Representation is already ended</h2>
          <p>This representation ended on {{ representativeRecord.validTo }}.</p>
        </section>
      } @else {
        @if (submissionError(); as errorMessage) {
          <section class="submission-error" role="alert">
            <h2>Ending unsuccessful</h2>
            <p>{{ errorMessage }}</p>
          </section>
        }

        <section aria-labelledby="confirmation-title" class="form-section">
          <h2 id="confirmation-title">Confirm</h2>
          <dl class="representative-summary">
            <div>
              <dt>Representative</dt>
              <dd>{{ representativeName() }}</dd>
            </div>
            <div>
              <dt>Valid from</dt>
              <dd>{{ representativeRecord.validFrom }}</dd>
            </div>
          </dl>
        </section>

        <form (ngSubmit)="confirm()" [formGroup]="endForm" novalidate>
          <section aria-labelledby="final-date-title" class="form-section">
            <h2 id="final-date-title">Final valid date</h2>
            <div class="form-field">
              <label for="valid-to">Valid to</label>
              <input
                [min]="representativeRecord.validFrom"
                id="valid-to"
                formControlName="validTo"
                type="date"
              />
              @if (endForm.controls.validTo.invalid && endForm.controls.validTo.touched) {
                <p class="field-error">Valid-to date is required.</p>
              }
              @if (dateError(); as errorMessage) {
                <p class="field-error">{{ errorMessage }}</p>
              }
            </div>
          </section>

          <footer class="form-actions">
            <a [routerLink]="customerLink()" class="button button--secondary">Cancel</a>
            <button [disabled]="submitting()" class="button button--primary" type="submit">
              {{ submitting() ? 'Ending...' : 'End Representation' }}
            </button>
          </footer>
        </form>
      }
    }
  }
</section>

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representation-end/legal-entity-representation-end.css

```
:host {
  display: block;
}

.representative-summary {
  display: grid;
  gap: 1rem;
  margin: 1rem 0 0;
}

.representative-summary div {
  padding: 1rem;
  background: #faf4f5;
  border: 1px solid #d7b7be;
  border-radius: 0.4rem;
}

.representative-summary dt {
  color: #650016;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
}

.representative-summary dd {
  margin: 0.35rem 0 0;
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-representation-end/legal-entity-representation-end.spec.ts

```
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityRepresentationEnd } from './legal-entity-representation-end';

describe('LegalEntityRepresentationEnd', () => {
  const customerId = '0199e65a-35b1-7000-8000-000000000102';
  const representativeId = '0199e65a-35b1-7000-8000-000000000201';
  const individualCustomerId = '0199e65a-35b1-7000-8000-000000000101';
  const representative: RepresentativeViewDto = {
    id: representativeId,
    individualCustomerId,
    firstName: 'Serafim',
    middleName: 'Ivanov',
    lastName: 'Gerasimoff',
    role: RepresentativeViewDto.RoleEnum.LegalRepresentative,
    validFrom: '2026-01-01',
    validTo: null,
  };
  const customer: LegalEntityCustomerViewDto = {
    id: customerId,
    customerNumber: 'CUS-000002',
    status: LegalEntityCustomerViewDto.StatusEnum.Active,
    email: 'office@starter.example.com',
    phone: '+35921234567',
    companyName: 'Starter Ltd',
    eik: '123456786',
    registeredAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
    correspondenceAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
    representatives: [representative],
  };
  const getLegalEntityCustomerMock = vi.fn();
  const getRepresentativeMock = vi.fn();
  const endRepresentationMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    getRepresentativeMock.mockReturnValue(of(representative));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityRepresentationEnd],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: customerId, representativeId }),
            },
          },
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
            getRepresentative: getRepresentativeMock,
            endRepresentation: endRepresentationMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and current Representative', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);

    // Act
    fixture.detectChanges();

    // Assert
    expect(getLegalEntityCustomerMock).toHaveBeenCalledWith({ id: customerId });
    expect(getRepresentativeMock).toHaveBeenCalledWith({ id: customerId, representativeId });
    expect(fixture.nativeElement.textContent).toContain('Serafim Ivanov Gerasimoff');
  });

  it('should end the representation and return to Customer details', () => {
    // Arrange
    endRepresentationMock.mockReturnValue(of({ ...representative, validTo: '2026-11-04' }));
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['endForm'].controls.validTo.setValue('2026-11-04');

    // Act
    component['confirm']();

    // Assert
    expect(endRepresentationMock).toHaveBeenCalledWith({
      id: customerId,
      representativeId,
      representationEndDto: { validTo: '2026-11-04' },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/legal-entities', customerId]);
  });

  it('should reject a final date before the first valid date', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['endForm'].controls.validTo.setValue('2025-12-31');

    // Act
    component['confirm']();
    fixture.detectChanges();

    // Assert
    expect(endRepresentationMock).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Valid-to date cannot be before the valid-from date.',
    );
  });

  it('should not offer ending for an already ended representation', () => {
    // Arrange
    getRepresentativeMock.mockReturnValue(of({ ...representative, validTo: '2026-10-31' }));
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Representation is already ended');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('should not show the form for a closed Customer', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(
      of({ ...customer, status: LegalEntityCustomerViewDto.StatusEnum.Closed }),
    );
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain('Customer is closed');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('should prevent repeat submission while pending', () => {
    // Arrange
    endRepresentationMock.mockReturnValue(new Subject<RepresentativeViewDto>());
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['endForm'].controls.validTo.setValue('2026-11-04');

    // Act
    component['confirm']();
    component['confirm']();

    // Assert
    expect(endRepresentationMock).toHaveBeenCalledOnce();
  });

  it('should show a conflict without navigating away', () => {
    // Arrange
    endRepresentationMock.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    const fixture = TestBed.createComponent(LegalEntityRepresentationEnd);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['endForm'].controls.validTo.setValue('2026-11-04');

    // Act
    component['confirm']();
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain(
      'The representation is already ended or the Customer state changed.',
    );
    expect(navigateMock).not.toHaveBeenCalled();
  });
});

```

