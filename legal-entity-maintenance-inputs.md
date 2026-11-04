## frontend/src/app/features/customers/customer-management.routes.ts

```
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

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-details/legal-entity-customer-details.ts

```
import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';

/**
 * Displays the details of one Legal Entity Customer.
 *
 * Every authenticated bank role may read the page. The response includes
 * current and ended Representatives; a Representative is shown as ended when
 * its inclusive valid-to date is before today. Maintenance actions are added
 * in a later batch.
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

    this.customerApi.getLegalEntityCustomer({id: customerId})
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
<section
  aria-labelledby="legal-entity-customer-details-title"
  class="customer-details"
>
  <a
    class="back-link"
    routerLink="/customers"
  >
    ← Back to customers
  </a>

  @if (loading()) {
    <p
      class="state-message"
      role="status"
    >
      Loading Legal Entity Customer...
    </p>
  } @else if (error(); as errorMessage) {
    <section
      class="state-panel state-panel--error"
      role="alert"
    >
      <h1 id="legal-entity-customer-details-title">Customer unavailable</h1>
      <p>{{ errorMessage }}</p>
      <button
        (click)="retry()"
        class="button button--primary"
        type="button"
      >
        Try again
      </button>
    </section>
  } @else if (customer(); as customerRecord) {
    <header class="customer-details__header">
      <div>
        <p class="customer-details__section-label">Legal Entity Customer</p>
        <p class="customer-details__field-label">Company name</p>
        <h1 id="legal-entity-customer-details-title">
          {{ customerRecord.companyName }}
        </h1>
        <p class="customer-details__field-label customer-details__number-label">
          Customer number
        </p>
        <p class="customer-details__number">
          {{ customerRecord.customerNumber }}
        </p>
      </div>
      <span
        [class.status-badge--closed]="customerRecord.status === 'CLOSED'"
        class="status-badge"
      >
        {{ customerRecord.status }}
      </span>
    </header>

    <section
      aria-labelledby="company-details-title"
      class="details-card"
    >
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
        {{ customerRecord.registeredAddress.addressLine1 }}<br>
        @if (customerRecord.registeredAddress.addressLine2) {
          {{ customerRecord.registeredAddress.addressLine2 }}<br>
        }
        {{ customerRecord.registeredAddress.postalCode }}
        {{ customerRecord.registeredAddress.city }}<br>
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
        {{ customerRecord.correspondenceAddress.addressLine1 }}<br>
        @if (customerRecord.correspondenceAddress.addressLine2) {
          {{ customerRecord.correspondenceAddress.addressLine2 }}<br>
        }
        {{ customerRecord.correspondenceAddress.postalCode }}
        {{ customerRecord.correspondenceAddress.city }}<br>
        {{ customerRecord.correspondenceAddress.countryCode }}
      </address>
    </section>

    <section
      aria-labelledby="representatives-title"
      class="details-card"
    >
      <h2 id="representatives-title">Representatives</h2>

      @if (customerRecord.representatives.length === 0) {
        <p class="representatives-empty">No representatives are recorded.</p>
      } @else {
        <div class="representatives-table-container">
          <table class="representatives-table">
            <caption class="visually-hidden">Representatives</caption>
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
import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import {LegalEntityCustomerDetails} from './legal-entity-customer-details';

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

  const getLegalEntityCustomerMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(customer));

    await TestBed.configureTestingModule({
      imports: [LegalEntityCustomerDetails],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({id: customerId}),
            },
          },
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
          },
        },
      ],
    }).compileComponents();
  });

  it('should request and render the Legal Entity Customer details', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(getLegalEntityCustomerMock).toHaveBeenCalledOnce();
    expect(getLegalEntityCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Starter Ltd');
    expect(element.textContent).toContain('CUS-000002');
    expect(element.textContent).toContain('123456786');
    expect(element.textContent).toContain('office@starter.example.com');
    expect(element.textContent).toContain('+35921234567');
    expect(element.textContent).toContain('ACTIVE');
  });

  it('should render both addresses', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    const registered = element.querySelector('#registered-address')?.textContent ?? '';
    const correspondence = element.querySelector('#correspondence-address')?.textContent ?? '';
    expect(registered).toContain('1 Alexander Battenberg Square');
    expect(registered).toContain('Floor 3');
    expect(correspondence).toContain('10 Main Street');
    expect(correspondence).toContain('Plovdiv');
  });

  it('should render current and ended Representatives', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const rows = Array.from(element.querySelectorAll('.representatives-table tbody tr'));

    // Assert
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
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const links = element.querySelectorAll<HTMLAnchorElement>('.representative-link');

    // Assert
    expect(links).toHaveLength(1);
    expect(links[0]?.getAttribute('href')).toBe(`/customers/individuals/${linkedIndividualId}`);
  });

  it('should show an empty message when no Representatives are recorded', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(of({...customer, representatives: []}));
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('No representatives are recorded.');
    expect(element.querySelector('.representatives-table')).toBeNull();
  });

  it('should not display the technical Customer identifier as text', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).not.toContain(customerId);
  });

  it('should show a loading state while the request is pending', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Loading Legal Entity Customer');
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show a not-found message for a missing Customer', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 404})),
    );
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('The requested Legal Entity Customer could not be found.');
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show an authorization message for a forbidden response', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403})),
    );
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain(
      'You do not have permission to view this Customer.',
    );
  });

  it('should retry loading the Legal Entity Customer', () => {
    // Arrange
    getLegalEntityCustomerMock
      .mockReturnValueOnce(throwError(() => new Error('Customer Service unavailable')))
      .mockReturnValueOnce(of(customer));
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const retryButton = element.querySelector('.state-panel .button') as HTMLButtonElement | null;

    // Act
    retryButton?.click();
    fixture.detectChanges();

    // Assert
    expect(getLegalEntityCustomerMock).toHaveBeenCalledTimes(2);
    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Starter Ltd');
    expect(element.querySelector('.state-panel--error')).toBeNull();
  });
});

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-registration/legal-entity-customer-registration.ts

```
import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {eikValidator} from '../../../../../core/forms/eik-validator';
import {notBlankValidator} from '../../../../../core/forms/not-blank-validator';
import {
  AddressDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerRegistrationDto,
  LegalEntityCustomerViewDto
} from '../../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../../generated/customer-schema-constraints.generated';

const registrationConstraints =
  customerSchemaConstraints.LegalEntityCustomerRegistrationDto;
const addressConstraints = customerSchemaConstraints.AddressDto;

type RegistrationControl =
  | 'email'
  | 'phone'
  | 'companyName'
  | 'eik';

type AddressGroup =
  | 'registeredAddress'
  | 'correspondenceAddress';

type AddressControl =
  | 'countryCode'
  | 'city'
  | 'postalCode'
  | 'addressLine1'
  | 'addressLine2';

interface AddressFormValue {
  readonly countryCode: string;
  readonly city: string;
  readonly postalCode: string;
  readonly addressLine1: string;
  readonly addressLine2: string;
}

/**
 * Registers a new Legal Entity Customer.
 *
 * The typed reactive form uses constraints generated from the Customer Service
 * OpenAPI contract and collects the registered and correspondence addresses.
 * Harmless edge whitespace is removed before validation. The EIK validator
 * provides immediate check-digit feedback, while Customer Service remains
 * authoritative for validation, uniqueness, authorization, and persistence.
 * Representatives are added separately after registration.
 */
@Component({
  selector: 'app-legal-entity-customer-registration',
  imports: [ReactiveFormsModule],
  templateUrl: './legal-entity-customer-registration.html',
  styleUrl: './legal-entity-customer-registration.css',
})
export class LegalEntityCustomerRegistration {
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly registrationForm = this.formBuilder.group({
    email: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.email,
      Validators.maxLength(registrationConstraints.email.maxLength),
    ]),
    phone: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.phone.maxLength),
    ]),
    companyName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.companyName.maxLength),
    ]),
    eik: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.minLength(registrationConstraints.eik.minLength),
      Validators.maxLength(registrationConstraints.eik.maxLength),
      Validators.pattern(registrationConstraints.eik.pattern),
      eikValidator(),
    ]),
    registeredAddress: this.createAddressGroup(),
    correspondenceAddress: this.createAddressGroup(),
  });

  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Normalizes, validates, and submits the registration form.
   *
   * Safe edge whitespace is removed before validation. Internal characters are
   * preserved, so an EIK containing an internal space remains invalid. A
   * successful response navigates to the new Customer details page.
   */
  protected submit(): void {
    if (this.submitting()) {
      return;
    }

    this.normalizeFormValues();

    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi.registerLegalEntityCustomer({
      legalEntityCustomerRegistrationDto: this.createRegistrationRequest(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          void this.router.navigate([
            '/customers/legal-entities',
            customer.id,
          ]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.errorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Reports whether a top-level registration control should display an error.
   *
   * @param controlName registration control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowError(controlName: RegistrationControl): boolean {
    const control = this.registrationForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Reports whether an address control should display an error.
   *
   * @param groupName address group name
   * @param controlName address control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowAddressError(groupName: AddressGroup, controlName: AddressControl): boolean {
    const control = this.registrationForm.controls[groupName].controls[controlName];
    return control.invalid && control.touched;
  }

  private createAddressGroup() {
    return this.formBuilder.group({
      countryCode: this.formBuilder.control('BG', [
        Validators.required,
        notBlankValidator(),
        Validators.pattern(addressConstraints.countryCode.pattern),
      ]),
      city: this.formBuilder.control('', [
        Validators.required,
        notBlankValidator(),
        Validators.maxLength(addressConstraints.city.maxLength),
      ]),
      postalCode: this.formBuilder.control('', [
        Validators.required,
        notBlankValidator(),
        Validators.maxLength(addressConstraints.postalCode.maxLength),
      ]),
      addressLine1: this.formBuilder.control('', [
        Validators.required,
        notBlankValidator(),
        Validators.maxLength(addressConstraints.addressLine1.maxLength),
      ]),
      addressLine2: this.formBuilder.control('', [
        Validators.maxLength(addressConstraints.addressLine2.maxLength),
      ]),
    });
  }

  private normalizeFormValues(): void {
    const value = this.registrationForm.getRawValue();

    this.registrationForm.patchValue({
      email: value.email.trim(),
      phone: value.phone.trim(),
      companyName: value.companyName.trim(),
      eik: value.eik.trim(),
      registeredAddress: this.normalizeAddress(value.registeredAddress),
      correspondenceAddress: this.normalizeAddress(value.correspondenceAddress),
    }, {
      emitEvent: false,
    });
  }

  private normalizeAddress(address: AddressFormValue): AddressFormValue {
    return {
      countryCode: address.countryCode.trim().toUpperCase(),
      city: address.city.trim(),
      postalCode: address.postalCode.trim(),
      addressLine1: address.addressLine1.trim(),
      addressLine2: address.addressLine2.trim(),
    };
  }

  private createRegistrationRequest(): LegalEntityCustomerRegistrationDto {
    const value = this.registrationForm.getRawValue();

    return {
      email: value.email,
      phone: value.phone,
      companyName: value.companyName,
      eik: value.eik,
      registeredAddress: this.toAddressDto(value.registeredAddress),
      correspondenceAddress: this.toAddressDto(value.correspondenceAddress),
    };
  }

  private toAddressDto(address: AddressFormValue): AddressDto {
    return {
      countryCode: address.countryCode,
      city: address.city,
      postalCode: address.postalCode,
      addressLine1: address.addressLine1,
      ...(address.addressLine2.length > 0
        ? {addressLine2: address.addressLine2}
        : {}),
    };
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Customer information is invalid. Review the form and try again.';
      }
      if (error.status === 409) {
        return 'This EIK is already registered to another Customer.';
      }
      if (error.status === 403) {
        return 'You do not have permission to register Customers.';
      }
    }
    return 'The Legal Entity Customer could not be registered. Please try again.';
  }
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-registration/legal-entity-customer-registration.html

```
<section
  aria-labelledby="legal-entity-registration-title"
  class="customer-maintenance"
>
  <a class="back-link" routerLink="/customers">← Back to customers</a>

  <header>
    <p class="customer-maintenance__label">Customer management</p>
    <h1 id="legal-entity-registration-title">Register Legal Entity Customer</h1>
    <p class="customer-maintenance__description">
      Create a Legal Entity Customer with contact details, a registered address,
      and a correspondence address. Representatives are added after registration.
    </p>
  </header>

  @if (submissionError(); as errorMessage) {
    <section class="submission-error" role="alert">
      <h2>Registration unsuccessful</h2>
      <p>{{ errorMessage }}</p>
    </section>
  }

  <form
    (ngSubmit)="submit()"
    [formGroup]="registrationForm"
    class="registration-form"
    novalidate
  >
    <section aria-labelledby="company-title" class="form-section">
      <h2 id="company-title">Company details</h2>

      <div class="form-grid">
        <div class="form-field form-field--full-width">
          <label for="company-name">Company name</label>
          <input
            aria-describedby="company-name-error"
            autocomplete="organization"
            formControlName="companyName"
            id="company-name"
            type="text"
          >
          @if (shouldShowError('companyName')) {
            <p class="field-error" id="company-name-error">
              Company name is required and must not exceed 200 characters.
            </p>
          }
        </div>

        <div class="form-field">
          <label for="eik">EIK</label>
          <input
            aria-describedby="eik-help eik-error"
            autocomplete="off"
            formControlName="eik"
            id="eik"
            inputmode="numeric"
            maxlength="9"
            type="text"
          >
          <p class="field-help" id="eik-help">
            Enter the 9-digit Bulgarian company identifier.
          </p>
          @if (shouldShowError('eik')) {
            <p class="field-error" id="eik-error">
              @if (registrationForm.controls.eik.hasError('required')) {
                EIK is required.
              } @else {
                Enter a valid 9-digit EIK with a correct check digit.
              }
            </p>
          }
        </div>
      </div>
    </section>

    <section aria-labelledby="contact-title" class="form-section">
      <h2 id="contact-title">Contact details</h2>

      <div class="form-grid">
        <div class="form-field">
          <label for="email">Email</label>
          <input
            aria-describedby="email-error"
            autocomplete="email"
            formControlName="email"
            id="email"
            type="email"
          >
          @if (shouldShowError('email')) {
            <p class="field-error" id="email-error">
              @if (registrationForm.controls.email.hasError('required')) {
                Email is required.
              } @else if (registrationForm.controls.email.hasError('email')) {
                Enter a valid email address.
              } @else {
                Email must not exceed 254 characters.
              }
            </p>
          }
        </div>

        <div class="form-field">
          <label for="phone">Phone</label>
          <input
            aria-describedby="phone-error"
            autocomplete="tel"
            formControlName="phone"
            id="phone"
            type="tel"
          >
          @if (shouldShowError('phone')) {
            <p class="field-error" id="phone-error">
              Phone is required and must not exceed 30 characters.
            </p>
          }
        </div>
      </div>
    </section>

    <section
      aria-labelledby="registered-address-title"
      class="form-section"
      formGroupName="registeredAddress"
    >
      <h2 id="registered-address-title">Registered address</h2>

      <div class="form-grid">
        <div class="form-field">
          <label for="registered-country-code">Country code</label>
          <input
            aria-describedby="registered-country-code-error"
            formControlName="countryCode"
            id="registered-country-code"
            maxlength="2"
            type="text"
          >
          @if (shouldShowAddressError('registeredAddress', 'countryCode')) {
            <p class="field-error" id="registered-country-code-error">
              Enter a two-letter country code.
            </p>
          }
        </div>

        <div class="form-field">
          <label for="registered-city">City</label>
          <input
            aria-describedby="registered-city-error"
            formControlName="city"
            id="registered-city"
            type="text"
          >
          @if (shouldShowAddressError('registeredAddress', 'city')) {
            <p class="field-error" id="registered-city-error">
              City is required and must not exceed 100 characters.
            </p>
          }
        </div>

        <div class="form-field">
          <label for="registered-postal-code">Postal code</label>
          <input
            aria-describedby="registered-postal-code-error"
            formControlName="postalCode"
            id="registered-postal-code"
            type="text"
          >
          @if (shouldShowAddressError('registeredAddress', 'postalCode')) {
            <p class="field-error" id="registered-postal-code-error">
              Postal code is required and must not exceed 20 characters.
            </p>
          }
        </div>

        <div class="form-field form-field--full-width">
          <label for="registered-address-line-1">Address line 1</label>
          <input
            aria-describedby="registered-address-line-1-error"
            formControlName="addressLine1"
            id="registered-address-line-1"
            type="text"
          >
          @if (shouldShowAddressError('registeredAddress', 'addressLine1')) {
            <p class="field-error" id="registered-address-line-1-error">
              Address line 1 is required and must not exceed 200 characters.
            </p>
          }
        </div>

        <div class="form-field form-field--full-width">
          <label for="registered-address-line-2">Address line 2</label>
          <input
            aria-describedby="registered-address-line-2-error"
            formControlName="addressLine2"
            id="registered-address-line-2"
            type="text"
          >
          @if (shouldShowAddressError('registeredAddress', 'addressLine2')) {
            <p class="field-error" id="registered-address-line-2-error">
              Address line 2 must not exceed 200 characters.
            </p>
          }
        </div>
      </div>
    </section>

    <section
      aria-labelledby="correspondence-address-title"
      class="form-section"
      formGroupName="correspondenceAddress"
    >
      <h2 id="correspondence-address-title">Correspondence address</h2>

      <div class="form-grid">
        <div class="form-field">
          <label for="correspondence-country-code">Country code</label>
          <input
            aria-describedby="correspondence-country-code-error"
            formControlName="countryCode"
            id="correspondence-country-code"
            maxlength="2"
            type="text"
          >
          @if (shouldShowAddressError('correspondenceAddress', 'countryCode')) {
            <p class="field-error" id="correspondence-country-code-error">
              Enter a two-letter country code.
            </p>
          }
        </div>

        <div class="form-field">
          <label for="correspondence-city">City</label>
          <input
            aria-describedby="correspondence-city-error"
            formControlName="city"
            id="correspondence-city"
            type="text"
          >
          @if (shouldShowAddressError('correspondenceAddress', 'city')) {
            <p class="field-error" id="correspondence-city-error">
              City is required and must not exceed 100 characters.
            </p>
          }
        </div>

        <div class="form-field">
          <label for="correspondence-postal-code">Postal code</label>
          <input
            aria-describedby="correspondence-postal-code-error"
            formControlName="postalCode"
            id="correspondence-postal-code"
            type="text"
          >
          @if (shouldShowAddressError('correspondenceAddress', 'postalCode')) {
            <p class="field-error" id="correspondence-postal-code-error">
              Postal code is required and must not exceed 20 characters.
            </p>
          }
        </div>

        <div class="form-field form-field--full-width">
          <label for="correspondence-address-line-1">Address line 1</label>
          <input
            aria-describedby="correspondence-address-line-1-error"
            formControlName="addressLine1"
            id="correspondence-address-line-1"
            type="text"
          >
          @if (shouldShowAddressError('correspondenceAddress', 'addressLine1')) {
            <p class="field-error" id="correspondence-address-line-1-error">
              Address line 1 is required and must not exceed 200 characters.
            </p>
          }
        </div>

        <div class="form-field form-field--full-width">
          <label for="correspondence-address-line-2">Address line 2</label>
          <input
            aria-describedby="correspondence-address-line-2-error"
            formControlName="addressLine2"
            id="correspondence-address-line-2"
            type="text"
          >
          @if (shouldShowAddressError('correspondenceAddress', 'addressLine2')) {
            <p class="field-error" id="correspondence-address-line-2-error">
              Address line 2 must not exceed 200 characters.
            </p>
          }
        </div>
      </div>
    </section>

    <footer class="form-actions">
      <a class="button button--secondary" routerLink="/customers">Cancel</a>
      <button
        [disabled]="submitting()"
        class="button button--primary"
        type="submit"
      >
        @if (submitting()) {
          Registering...
        } @else {
          Register Legal Entity Customer
        }
      </button>
    </footer>
  </form>
</section>

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-registration/legal-entity-customer-registration.css

```
:host {
  display: block;
}

```

## frontend/src/app/features/customers/legal-entity/pages/legal-entity-customer-registration/legal-entity-customer-registration.spec.ts

```
import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {LegalEntityCustomerAPIService, LegalEntityCustomerViewDto} from '../../../../../generated/customer-api';
import {LegalEntityCustomerRegistration} from './legal-entity-customer-registration';

describe('LegalEntityCustomerRegistration', () => {
  const registeredCustomer: LegalEntityCustomerViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000104',
    customerNumber: 'CUS-000004',
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
      city: 'Plovdiv',
      postalCode: '4000',
      addressLine1: '10 Main Street',
      addressLine2: 'Office 5',
    },
    representatives: [],
  };

  const validFormValue = {
    email: 'office@starter.example.com',
    phone: '+35921234567',
    companyName: 'Starter Ltd',
    eik: '123456786',
    registeredAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: '',
    },
    correspondenceAddress: {
      countryCode: 'BG',
      city: 'Plovdiv',
      postalCode: '4000',
      addressLine1: '10 Main Street',
      addressLine2: 'Office 5',
    },
  };

  const registerMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    registerMock.mockReturnValue(of(registeredCustomer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityCustomerRegistration],
      providers: [
        provideRouter([]),
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            registerLegalEntityCustomer: registerMock,
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(navigateMock);
  });

  it('should render all registration sections', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Register Legal Entity Customer');
    expect(element.textContent).toContain('Company details');
    expect(element.textContent).toContain('Contact details');
    expect(element.textContent).toContain('Registered address');
    expect(element.textContent).toContain('Correspondence address');
    expect(element.querySelector('#eik')).not.toBeNull();
    expect(element.querySelector('#registered-address-line-1')).not.toBeNull();
    expect(element.querySelector('#correspondence-address-line-1')).not.toBeNull();
  });

  it('should reject an EIK with a wrong check digit without calling Customer Service', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue({...validFormValue, eik: '123456789'});

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].controls.eik.hasError('eik')).toBe(true);
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('should keep an EIK with internal whitespace invalid', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue({...validFormValue, eik: '1234 56786'});

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].controls.eik.value).toBe('1234 56786');
    expect(component['registrationForm'].controls.eik.invalid).toBe(true);
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('should mark invalid controls in both addresses as touched', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();

    // Assert
    const controls = component['registrationForm'].controls;
    expect(controls.companyName.touched).toBe(true);
    expect(controls.registeredAddress.controls.city.touched).toBe(true);
    expect(controls.correspondenceAddress.controls.city.touched).toBe(true);
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('should normalize, validate, and submit the canonical registration', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue({
      email: '  office@starter.example.com  ',
      phone: '  +35921234567  ',
      companyName: '  Starter Ltd  ',
      eik: '  123456786  ',
      registeredAddress: {
        countryCode: '  bg  ',
        city: '  Sofia  ',
        postalCode: '  1000  ',
        addressLine1: '  1 Alexander Battenberg Square  ',
        addressLine2: '   ',
      },
      correspondenceAddress: {
        countryCode: ' bg ',
        city: ' Plovdiv ',
        postalCode: ' 4000 ',
        addressLine1: ' 10 Main Street ',
        addressLine2: ' Office 5 ',
      },
    });

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].getRawValue()).toEqual(validFormValue);
    expect(registerMock).toHaveBeenCalledWith({
      legalEntityCustomerRegistrationDto: {
        email: 'office@starter.example.com',
        phone: '+35921234567',
        companyName: 'Starter Ltd',
        eik: '123456786',
        registeredAddress: {
          countryCode: 'BG',
          city: 'Sofia',
          postalCode: '1000',
          addressLine1: '1 Alexander Battenberg Square',
        },
        correspondenceAddress: {
          countryCode: 'BG',
          city: 'Plovdiv',
          postalCode: '4000',
          addressLine1: '10 Main Street',
          addressLine2: 'Office 5',
        },
      },
    });
    expect(navigateMock).toHaveBeenCalledWith([
      '/customers/legal-entities',
      registeredCustomer.id,
    ]);
  });

  it('should disable repeat submission while the request is pending', () => {
    // Arrange
    registerMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue(validFormValue);

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(registerMock).toHaveBeenCalledOnce();
    expect(component['submitting']()).toBe(true);
  });

  it('should show the duplicate-EIK message for a conflict', () => {
    // Arrange
    registerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409})),
    );
    const fixture = TestBed.createComponent(LegalEntityCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue(validFormValue);

    // Act
    component['submit']();
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain(
      'This EIK is already registered to another Customer.',
    );
    expect(component['submitting']()).toBe(false);
  });
});

```

## frontend/src/app/generated/customer-api/api/legal-entity-customer-api.service.ts

```
/**
 * Customer Service API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
/* tslint:disable:no-unused-variable member-ordering */

import { Inject, Injectable, Optional }                      from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams,
         HttpResponse, HttpEvent, HttpContext 
        }       from '@angular/common/http';
import { Observable }                                        from 'rxjs';
import { OpenApiHttpParams, QueryParamStyle } from '../query.params';

// @ts-ignore
import { AddressDto } from '../model/address-dto';
// @ts-ignore
import { CompanyNameUpdateDto } from '../model/company-name-update-dto';
// @ts-ignore
import { CustomerContactUpdateDto } from '../model/customer-contact-update-dto';
// @ts-ignore
import { EikCorrectionDto } from '../model/eik-correction-dto';
// @ts-ignore
import { LegalEntityCustomerRegistrationDto } from '../model/legal-entity-customer-registration-dto';
// @ts-ignore
import { LegalEntityCustomerViewDto } from '../model/legal-entity-customer-view-dto';
// @ts-ignore
import { ProblemDetail } from '../model/problem-detail';
// @ts-ignore
import { RepresentationEndDto } from '../model/representation-end-dto';
// @ts-ignore
import { RepresentativeAdditionDto } from '../model/representative-addition-dto';
// @ts-ignore
import { RepresentativeCorrectionDto } from '../model/representative-correction-dto';
// @ts-ignore
import { RepresentativeViewDto } from '../model/representative-view-dto';

// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS }                     from '../variables';
import { Configuration }                                     from '../configuration';
import { BaseService } from '../api.base.service';


export interface AddRepresentativeRequestParams {
    id: string;
    representativeAdditionDto: RepresentativeAdditionDto;
}

export interface ChangeLegalEntityCustomerCorrespondenceAddressRequestParams {
    id: string;
    addressDto: AddressDto;
}

export interface ChangeLegalEntityCustomerRegisteredAddressRequestParams {
    id: string;
    addressDto: AddressDto;
}

export interface CloseLegalEntityCustomerRequestParams {
    id: string;
}

export interface CorrectLegalEntityCustomerEikRequestParams {
    id: string;
    eikCorrectionDto: EikCorrectionDto;
}

export interface CorrectRepresentativeRequestParams {
    id: string;
    representativeId: string;
    representativeCorrectionDto: RepresentativeCorrectionDto;
}

export interface EndRepresentationRequestParams {
    id: string;
    representativeId: string;
    representationEndDto: RepresentationEndDto;
}

export interface GetLegalEntityCustomerRequestParams {
    /** Technical customer UUID */
    id: string;
}

export interface GetRepresentativeRequestParams {
    id: string;
    representativeId: string;
}

export interface RegisterLegalEntityCustomerRequestParams {
    legalEntityCustomerRegistrationDto: LegalEntityCustomerRegistrationDto;
}

export interface ReopenLegalEntityCustomerRequestParams {
    id: string;
}

export interface UpdateLegalEntityCustomerCompanyNameRequestParams {
    id: string;
    companyNameUpdateDto: CompanyNameUpdateDto;
}

export interface UpdateLegalEntityCustomerContactRequestParams {
    id: string;
    customerContactUpdateDto: CustomerContactUpdateDto;
}


@Injectable({
  providedIn: 'root'
})
export class LegalEntityCustomerAPIService extends BaseService {

    constructor(protected httpClient: HttpClient, @Optional() @Inject(BASE_PATH) basePath: string|string[], @Optional() configuration?: Configuration) {
        super(basePath, configuration);
    }

    /**
     * Add a representative
     * Supply either individualCustomerId, whose names are copied, or the names, but not both.
     * @endpoint post /api/customers/legal-entities/{id}/representatives
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public addRepresentative(requestParameters: AddRepresentativeRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<RepresentativeViewDto>;
    public addRepresentative(requestParameters: AddRepresentativeRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<RepresentativeViewDto>>;
    public addRepresentative(requestParameters: AddRepresentativeRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<RepresentativeViewDto>>;
    public addRepresentative(requestParameters: AddRepresentativeRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling addRepresentative.');
        }
        const representativeAdditionDto = requestParameters?.representativeAdditionDto;
        if (representativeAdditionDto === null || representativeAdditionDto === undefined) {
            throw new Error('Required parameter representativeAdditionDto was null or undefined when calling addRepresentative.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/representatives`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<RepresentativeViewDto>('post', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: representativeAdditionDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Replace the correspondence address
     * @endpoint put /api/customers/legal-entities/{id}/correspondence-address
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public changeLegalEntityCustomerCorrespondenceAddress(requestParameters: ChangeLegalEntityCustomerCorrespondenceAddressRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public changeLegalEntityCustomerCorrespondenceAddress(requestParameters: ChangeLegalEntityCustomerCorrespondenceAddressRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public changeLegalEntityCustomerCorrespondenceAddress(requestParameters: ChangeLegalEntityCustomerCorrespondenceAddressRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public changeLegalEntityCustomerCorrespondenceAddress(requestParameters: ChangeLegalEntityCustomerCorrespondenceAddressRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling changeLegalEntityCustomerCorrespondenceAddress.');
        }
        const addressDto = requestParameters?.addressDto;
        if (addressDto === null || addressDto === undefined) {
            throw new Error('Required parameter addressDto was null or undefined when calling changeLegalEntityCustomerCorrespondenceAddress.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/correspondence-address`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('put', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: addressDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Replace the registered address
     * @endpoint put /api/customers/legal-entities/{id}/registered-address
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public changeLegalEntityCustomerRegisteredAddress(requestParameters: ChangeLegalEntityCustomerRegisteredAddressRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public changeLegalEntityCustomerRegisteredAddress(requestParameters: ChangeLegalEntityCustomerRegisteredAddressRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public changeLegalEntityCustomerRegisteredAddress(requestParameters: ChangeLegalEntityCustomerRegisteredAddressRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public changeLegalEntityCustomerRegisteredAddress(requestParameters: ChangeLegalEntityCustomerRegisteredAddressRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling changeLegalEntityCustomerRegisteredAddress.');
        }
        const addressDto = requestParameters?.addressDto;
        if (addressDto === null || addressDto === undefined) {
            throw new Error('Required parameter addressDto was null or undefined when calling changeLegalEntityCustomerRegisteredAddress.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/registered-address`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('put', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: addressDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Close the customer relationship
     * Representations are left unchanged.
     * @endpoint post /api/customers/legal-entities/{id}/close
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public closeLegalEntityCustomer(requestParameters: CloseLegalEntityCustomerRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public closeLegalEntityCustomer(requestParameters: CloseLegalEntityCustomerRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public closeLegalEntityCustomer(requestParameters: CloseLegalEntityCustomerRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public closeLegalEntityCustomer(requestParameters: CloseLegalEntityCustomerRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling closeLegalEntityCustomer.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/close`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('post', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Correct a wrongly entered EIK
     * @endpoint put /api/customers/legal-entities/{id}/eik
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public correctLegalEntityCustomerEik(requestParameters: CorrectLegalEntityCustomerEikRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public correctLegalEntityCustomerEik(requestParameters: CorrectLegalEntityCustomerEikRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public correctLegalEntityCustomerEik(requestParameters: CorrectLegalEntityCustomerEikRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public correctLegalEntityCustomerEik(requestParameters: CorrectLegalEntityCustomerEikRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling correctLegalEntityCustomerEik.');
        }
        const eikCorrectionDto = requestParameters?.eikCorrectionDto;
        if (eikCorrectionDto === null || eikCorrectionDto === undefined) {
            throw new Error('Required parameter eikCorrectionDto was null or undefined when calling correctLegalEntityCustomerEik.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/eik`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('put', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: eikCorrectionDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Correct a data-entry mistake in a representative
     * Replaces every recorded value. A null validTo reopens a representation ended by mistake.
     * @endpoint put /api/customers/legal-entities/{id}/representatives/{representativeId}
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public correctRepresentative(requestParameters: CorrectRepresentativeRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<RepresentativeViewDto>;
    public correctRepresentative(requestParameters: CorrectRepresentativeRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<RepresentativeViewDto>>;
    public correctRepresentative(requestParameters: CorrectRepresentativeRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<RepresentativeViewDto>>;
    public correctRepresentative(requestParameters: CorrectRepresentativeRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling correctRepresentative.');
        }
        const representativeId = requestParameters?.representativeId;
        if (representativeId === null || representativeId === undefined) {
            throw new Error('Required parameter representativeId was null or undefined when calling correctRepresentative.');
        }
        const representativeCorrectionDto = requestParameters?.representativeCorrectionDto;
        if (representativeCorrectionDto === null || representativeCorrectionDto === undefined) {
            throw new Error('Required parameter representativeCorrectionDto was null or undefined when calling correctRepresentative.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/representatives/${this.configuration.encodeParam({name: "representativeId", value: representativeId, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<RepresentativeViewDto>('put', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: representativeCorrectionDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * End a representative\&#39;s authority
     * @endpoint post /api/customers/legal-entities/{id}/representatives/{representativeId}/end
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public endRepresentation(requestParameters: EndRepresentationRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<RepresentativeViewDto>;
    public endRepresentation(requestParameters: EndRepresentationRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<RepresentativeViewDto>>;
    public endRepresentation(requestParameters: EndRepresentationRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<RepresentativeViewDto>>;
    public endRepresentation(requestParameters: EndRepresentationRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling endRepresentation.');
        }
        const representativeId = requestParameters?.representativeId;
        if (representativeId === null || representativeId === undefined) {
            throw new Error('Required parameter representativeId was null or undefined when calling endRepresentation.');
        }
        const representationEndDto = requestParameters?.representationEndDto;
        if (representationEndDto === null || representationEndDto === undefined) {
            throw new Error('Required parameter representationEndDto was null or undefined when calling endRepresentation.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/representatives/${this.configuration.encodeParam({name: "representativeId", value: representativeId, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/end`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<RepresentativeViewDto>('post', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: representationEndDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Get a legal-entity customer
     * Includes current and ended representatives.
     * @endpoint get /api/customers/legal-entities/{id}
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public getLegalEntityCustomer(requestParameters: GetLegalEntityCustomerRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public getLegalEntityCustomer(requestParameters: GetLegalEntityCustomerRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public getLegalEntityCustomer(requestParameters: GetLegalEntityCustomerRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public getLegalEntityCustomer(requestParameters: GetLegalEntityCustomerRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling getLegalEntityCustomer.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('get', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Get a representative
     * @endpoint get /api/customers/legal-entities/{id}/representatives/{representativeId}
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public getRepresentative(requestParameters: GetRepresentativeRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<RepresentativeViewDto>;
    public getRepresentative(requestParameters: GetRepresentativeRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<RepresentativeViewDto>>;
    public getRepresentative(requestParameters: GetRepresentativeRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<RepresentativeViewDto>>;
    public getRepresentative(requestParameters: GetRepresentativeRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling getRepresentative.');
        }
        const representativeId = requestParameters?.representativeId;
        if (representativeId === null || representativeId === undefined) {
            throw new Error('Required parameter representativeId was null or undefined when calling getRepresentative.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/representatives/${this.configuration.encodeParam({name: "representativeId", value: representativeId, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<RepresentativeViewDto>('get', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Register a legal-entity customer
     * The customer number is generated by the service. Representatives are added separately.
     * @endpoint post /api/customers/legal-entities
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public registerLegalEntityCustomer(requestParameters: RegisterLegalEntityCustomerRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public registerLegalEntityCustomer(requestParameters: RegisterLegalEntityCustomerRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public registerLegalEntityCustomer(requestParameters: RegisterLegalEntityCustomerRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public registerLegalEntityCustomer(requestParameters: RegisterLegalEntityCustomerRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const legalEntityCustomerRegistrationDto = requestParameters?.legalEntityCustomerRegistrationDto;
        if (legalEntityCustomerRegistrationDto === null || legalEntityCustomerRegistrationDto === undefined) {
            throw new Error('Required parameter legalEntityCustomerRegistrationDto was null or undefined when calling registerLegalEntityCustomer.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('post', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: legalEntityCustomerRegistrationDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Reopen a closed customer relationship
     * @endpoint post /api/customers/legal-entities/{id}/reopen
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public reopenLegalEntityCustomer(requestParameters: ReopenLegalEntityCustomerRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public reopenLegalEntityCustomer(requestParameters: ReopenLegalEntityCustomerRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public reopenLegalEntityCustomer(requestParameters: ReopenLegalEntityCustomerRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public reopenLegalEntityCustomer(requestParameters: ReopenLegalEntityCustomerRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling reopenLegalEntityCustomer.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/reopen`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('post', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Update the registered company name
     * @endpoint put /api/customers/legal-entities/{id}/company-name
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public updateLegalEntityCustomerCompanyName(requestParameters: UpdateLegalEntityCustomerCompanyNameRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public updateLegalEntityCustomerCompanyName(requestParameters: UpdateLegalEntityCustomerCompanyNameRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public updateLegalEntityCustomerCompanyName(requestParameters: UpdateLegalEntityCustomerCompanyNameRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public updateLegalEntityCustomerCompanyName(requestParameters: UpdateLegalEntityCustomerCompanyNameRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling updateLegalEntityCustomerCompanyName.');
        }
        const companyNameUpdateDto = requestParameters?.companyNameUpdateDto;
        if (companyNameUpdateDto === null || companyNameUpdateDto === undefined) {
            throw new Error('Required parameter companyNameUpdateDto was null or undefined when calling updateLegalEntityCustomerCompanyName.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/company-name`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('put', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: companyNameUpdateDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Update the customer\&#39;s email and phone
     * @endpoint put /api/customers/legal-entities/{id}/contact
     * @param requestParameters
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     * @param options additional options
     */
    public updateLegalEntityCustomerContact(requestParameters: UpdateLegalEntityCustomerContactRequestParams, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<LegalEntityCustomerViewDto>;
    public updateLegalEntityCustomerContact(requestParameters: UpdateLegalEntityCustomerContactRequestParams, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpResponse<LegalEntityCustomerViewDto>>;
    public updateLegalEntityCustomerContact(requestParameters: UpdateLegalEntityCustomerContactRequestParams, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<HttpEvent<LegalEntityCustomerViewDto>>;
    public updateLegalEntityCustomerContact(requestParameters: UpdateLegalEntityCustomerContactRequestParams, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json' | 'application/problem+json', context?: HttpContext, transferCache?: boolean}): Observable<any> {
        const id = requestParameters?.id;
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling updateLegalEntityCustomerContact.');
        }
        const customerContactUpdateDto = requestParameters?.customerContactUpdateDto;
        if (customerContactUpdateDto === null || customerContactUpdateDto === undefined) {
            throw new Error('Required parameter customerContactUpdateDto was null or undefined when calling updateLegalEntityCustomerContact.');
        }

        let localVarHeaders = this.defaultHeaders;

        // authentication (bearerAuth) required
        localVarHeaders = this.configuration.addCredentialToHeaders('bearerAuth', 'Authorization', localVarHeaders, 'Bearer ');

        const localVarHttpHeaderAcceptSelected: string | undefined = options?.httpHeaderAccept ?? this.configuration.selectHeaderAccept([
            'application/json',
            'application/problem+json'
        ]);
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        const localVarHttpContext: HttpContext = options?.context ?? new HttpContext();

        const localVarTransferCache: boolean = options?.transferCache ?? true;


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/customers/legal-entities/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: "uuid"})}/contact`;
        const { basePath, withCredentials } = this.configuration;
        return this.httpClient.request<LegalEntityCustomerViewDto>('put', `${basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: customerContactUpdateDto,
                responseType: <any>responseType_,
                ...(withCredentials ? { withCredentials } : {}),
                headers: localVarHeaders,
                observe: observe,
                ...(localVarTransferCache !== undefined ? { transferCache: localVarTransferCache } : {}),
                reportProgress: reportProgress
            }
        );
    }

}

```

## frontend/src/app/generated/customer-api/model/company-name-update-dto.ts

```
/**
 * Customer Service API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


export interface CompanyNameUpdateDto { 
    /**
     * Registered company name.
     */
    companyName: string;
}


```

## frontend/src/app/generated/customer-api/model/customer-contact-update-dto.ts

```
/**
 * Customer Service API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


export interface CustomerContactUpdateDto { 
    /**
     * Customer email.
     */
    email: string;
    /**
     * Customer phone.
     */
    phone: string;
}


```

## frontend/src/app/generated/customer-api/model/address-dto.ts

```
/**
 * Customer Service API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


export interface AddressDto { 
    /**
     * Two-letter country code.
     */
    countryCode: string;
    /**
     * City or locality.
     */
    city: string;
    /**
     * Postal routing code.
     */
    postalCode: string;
    /**
     * Primary address description.
     */
    addressLine1: string;
    /**
     * Optional additional address description.
     */
    addressLine2?: string;
}


```

## frontend/src/app/generated/customer-api/model/eik-correction-dto.ts

```
/**
 * Customer Service API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


export interface EikCorrectionDto { 
    /**
     * Correct 9-digit Bulgarian legal-entity identifier (EIK).
     */
    eik: string;
}


```

## frontend/src/app/generated/customer-api/model/legal-entity-customer-view-dto.ts

```
/**
 * Customer Service API
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { AddressViewDto } from './address-view-dto';
import { RepresentativeViewDto } from './representative-view-dto';


export interface LegalEntityCustomerViewDto { 
    /**
     * Technical Customer identifier.
     */
    id: string;
    /**
     * Business Customer number.
     */
    customerNumber: string;
    /**
     * Customer status.
     */
    status: LegalEntityCustomerViewDto.StatusEnum;
    /**
     * Customer email.
     */
    email: string;
    /**
     * Customer phone.
     */
    phone: string;
    /**
     * Registered company name.
     */
    companyName: string;
    /**
     * 9-digit Bulgarian legal-entity identifier (EIK).
     */
    eik: string;
    /**
     * Registered company address.
     */
    registeredAddress: AddressViewDto;
    /**
     * Correspondence address.
     */
    correspondenceAddress: AddressViewDto;
    /**
     * Current and ended Representatives.
     */
    representatives: Array<RepresentativeViewDto>;
}
export namespace LegalEntityCustomerViewDto {
    export const StatusEnum = {
        Active: 'ACTIVE',
        Closed: 'CLOSED',
    } as const;
    export type StatusEnum = typeof StatusEnum[keyof typeof StatusEnum];
}



```

## frontend/src/app/generated/customer-schema-constraints.generated.ts

```
/*
 * Auto-generated from openapi/customer-service.json.
 * Do not edit manually.
 */
export const customerSchemaConstraints = {
  "AddressDto": {
    "countryCode": {
      "required": true,
      "minLength": 1,
      "pattern": "[A-Za-z]{2}"
    },
    "city": {
      "required": true,
      "maxLength": 100,
      "minLength": 0
    },
    "postalCode": {
      "required": true,
      "maxLength": 20,
      "minLength": 0
    },
    "addressLine1": {
      "required": true,
      "maxLength": 200,
      "minLength": 0
    },
    "addressLine2": {
      "required": false,
      "maxLength": 200,
      "minLength": 0
    }
  },
  "CompanyNameUpdateDto": {
    "companyName": {
      "required": true,
      "maxLength": 200,
      "minLength": 0
    }
  },
  "CustomerContactUpdateDto": {
    "email": {
      "required": true,
      "maxLength": 254,
      "minLength": 0,
      "format": "email"
    },
    "phone": {
      "required": true,
      "maxLength": 30,
      "minLength": 0
    }
  },
  "EgnCorrectionDto": {
    "egn": {
      "required": true,
      "maxLength": 10,
      "minLength": 10,
      "pattern": "\\d{10}"
    }
  },
  "EikCorrectionDto": {
    "eik": {
      "required": true,
      "maxLength": 9,
      "minLength": 9,
      "pattern": "\\d{9}"
    }
  },
  "IndividualCustomerNameUpdateDto": {
    "firstName": {
      "required": true,
      "maxLength": 100,
      "minLength": 0
    },
    "middleName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "lastName": {
      "required": true,
      "maxLength": 100,
      "minLength": 0
    }
  },
  "IndividualCustomerRegistrationDto": {
    "email": {
      "required": true,
      "maxLength": 254,
      "minLength": 0,
      "format": "email"
    },
    "phone": {
      "required": true,
      "maxLength": 30,
      "minLength": 0
    },
    "firstName": {
      "required": true,
      "maxLength": 100,
      "minLength": 0
    },
    "middleName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "lastName": {
      "required": true,
      "maxLength": 100,
      "minLength": 0
    },
    "egn": {
      "required": true,
      "maxLength": 10,
      "minLength": 10,
      "pattern": "\\d{10}"
    },
    "residentialAddress": {
      "required": true
    }
  },
  "LegalEntityCustomerRegistrationDto": {
    "email": {
      "required": true,
      "maxLength": 254,
      "minLength": 0,
      "format": "email"
    },
    "phone": {
      "required": true,
      "maxLength": 30,
      "minLength": 0
    },
    "companyName": {
      "required": true,
      "maxLength": 200,
      "minLength": 0
    },
    "eik": {
      "required": true,
      "maxLength": 9,
      "minLength": 9,
      "pattern": "\\d{9}"
    },
    "registeredAddress": {
      "required": true
    },
    "correspondenceAddress": {
      "required": true
    }
  },
  "RepresentationEndDto": {
    "validTo": {
      "required": true,
      "format": "date"
    }
  },
  "RepresentativeAdditionDto": {
    "individualCustomerId": {
      "required": false,
      "format": "uuid"
    },
    "firstName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "middleName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "lastName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "role": {
      "required": true
    },
    "validFrom": {
      "required": true,
      "format": "date"
    }
  },
  "RepresentativeCorrectionDto": {
    "individualCustomerId": {
      "required": false,
      "format": "uuid"
    },
    "firstName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "middleName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "lastName": {
      "required": false,
      "maxLength": 100,
      "minLength": 0
    },
    "role": {
      "required": true
    },
    "validFrom": {
      "required": true,
      "format": "date"
    },
    "validTo": {
      "required": false,
      "format": "date"
    }
  }
} as const;

```

## frontend/src/app/core/forms/eik-validator.ts

```
import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

const EIK_PATTERN = /^\d{9}$/;
const FIRST_PASS_WEIGHTS = [1, 2, 3, 4, 5, 6, 7, 8];
const SECOND_PASS_WEIGHTS = [3, 4, 5, 6, 7, 8, 9, 10];

/**
 * Creates a validator for a 9-digit Bulgarian EIK.
 *
 * The ninth digit is a check digit. The weighted sum of the first eight
 * digits is taken modulo 11 with weights 1 to 8. A remainder of 10 is
 * recalculated with weights 3 to 10, and a second remainder of 10 becomes 0.
 * Empty values are accepted so that required-field validation stays separate.
 * Customer Service remains authoritative.
 *
 * @returns a validator reporting {@code eik} for an invalid value
 */
export function eikValidator(): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === undefined || value.length === 0) {
      return null;
    }
    return isValidEik(value) ? null : {eik: true};
  };
}

function isValidEik(value: string): boolean {
  if (!EIK_PATTERN.test(value)) {
    return false;
  }
  const digits = Array.from(value, Number);
  let checkDigit = weightedRemainder(digits, FIRST_PASS_WEIGHTS);
  if (checkDigit === 10) {
    checkDigit = weightedRemainder(digits, SECOND_PASS_WEIGHTS);
    if (checkDigit === 10) {
      checkDigit = 0;
    }
  }
  return checkDigit === digits[8];
}

function weightedRemainder(digits: readonly number[], weights: readonly number[]): number {
  const sum = weights.reduce((total, weight, index) => total + weight * (digits[index] ?? 0), 0);
  return sum % 11;
}

```

## frontend/src/app/core/forms/eik-validator.spec.ts

```
import {FormControl} from '@angular/forms';
import {describe, expect, it} from 'vitest';
import {eikValidator} from './eik-validator';

describe('eikValidator', () => {
  const validate = (value: string) =>
    eikValidator()(new FormControl(value, {nonNullable: true}));

  it('should accept an EIK with a valid first-pass check digit', () => {
    // Act
    const result = validate('123456786');

    // Assert
    expect(result).toBeNull();
  });

  it('should accept an EIK that needs the second-pass weights', () => {
    // Act
    const result = validate('100000086');

    // Assert
    expect(result).toBeNull();
  });

  it('should reject an EIK with a wrong check digit', () => {
    // Act
    const result = validate('123456789');

    // Assert
    expect(result).toEqual({eik: true});
  });

  it.each([
    ['12345678'],
    ['1234567860'],
    ['12345678a'],
    ['1234 5678'],
  ])('should reject the malformed value %s', (value) => {
    // Act
    const result = validate(value);

    // Assert
    expect(result).toEqual({eik: true});
  });

  it('should leave an empty value to the required validator', () => {
    // Act
    const result = validate('');

    // Assert
    expect(result).toBeNull();
  });
});

```

## frontend/src/styles.css

```
/* Shared application styles. */

.back-link {
  width: fit-content;
  color: #8f001f;
  font-weight: 700;
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}

.back-link:focus-visible,
.button:focus-visible {
  outline: 0.2rem solid #d7193f;
  outline-offset: 0.2rem;
}

/* Customer maintenance page shell shared by every single-operation page. */

.customer-maintenance {
  display: grid;
  gap: 1.5rem;
  max-width: 55rem;
}

.customer-maintenance__label {
  margin: 0 0 0.4rem;
  color: #8f001f;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.customer-maintenance h1 {
  margin: 0;
  color: #650016;
  font-size: clamp(1.75rem, 4vw, 2.4rem);
  line-height: 1.2;
}

.customer-maintenance__description {
  margin: 0.75rem 0 0;
  color: #626262;
  line-height: 1.5;
}

.customer-maintenance__status {
  margin: 0;
  padding: 1.5rem;
  background: #ffffff;
  border: 1px solid #dddddd;
  border-radius: 0.5rem;
}

.customer-maintenance__summary {
  margin: 1rem 0 0;
  color: #242424;
  font-weight: 700;
}

/* Customer details pages. */

.customer-details {
  display: grid;
  gap: 1.5rem;
}

.customer-details__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
}

.customer-details__section-label {
  margin: 0 0 1rem;
  color: #8f001f;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.customer-details__field-label {
  margin: 0 0 0.3rem;
  color: #626262;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.customer-details h1 {
  margin: 0;
  color: #650016;
  font-size: clamp(1.75rem, 4vw, 2.4rem);
  line-height: 1.2;
}

.customer-details__number-label {
  margin-top: 1rem;
}

.customer-details__number {
  margin: 0;
  color: #242424;
  font-size: 1rem;
  font-weight: 700;
}

.customer-details .customer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.customer-details .details-card,
.customer-details .state-message,
.customer-details .state-panel {
  padding: 1.5rem;
  background: #ffffff;
  border: 1px solid #dddddd;
  border-radius: 0.5rem;
  box-shadow: 0 0.25rem 1rem rgb(0 0 0 / 6%);
}

.customer-details .details-card h2,
.customer-details .state-panel h1 {
  margin: 0;
  color: #650016;
  font-size: 1.25rem;
}

.customer-details .details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 15rem), 1fr));
  gap: 1.5rem;
  margin: 1.5rem 0 0;
}

.customer-details .details-grid__item {
  min-width: 0;
}

.customer-details .details-grid dt {
  color: #626262;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.customer-details .details-grid dd {
  margin: 0.35rem 0 0;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.customer-details .address {
  margin-top: 1.5rem;
  font-style: normal;
  font-weight: 600;
  line-height: 1.6;
}

.customer-details .status-badge {
  display: inline-block;
  flex-shrink: 0;
  padding: 0.3rem 0.6rem;
  color: #14532d;
  background: #dcfce7;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.customer-details .status-badge--closed {
  color: #7f1d1d;
  background: #fee2e2;
}

.customer-details .state-message {
  margin: 0;
}

.customer-details .state-panel p {
  margin: 0.75rem 0 0;
  color: #626262;
}

.customer-details .state-panel--error {
  border-color: #d7193f;
}

.customer-details .state-panel .button {
  margin-top: 1rem;
}

/* Shared form primitives. */

.registration-form {
  display: grid;
  gap: 1.5rem;
}

.form-section,
.submission-error {
  padding: 1.5rem;
  background: #ffffff;
  border: 1px solid #dddddd;
  border-radius: 0.5rem;
  box-shadow: 0 0.25rem 1rem rgb(0 0 0 / 6%);
}

.form-section h2,
.submission-error h2 {
  margin: 0;
  color: #650016;
  font-size: 1.25rem;
}

.form-section__description {
  margin: 0.6rem 0 0;
  color: #626262;
  line-height: 1.5;
}

.submission-error {
  border-color: #d7193f;
}

.submission-error p {
  margin: 0.75rem 0 0;
  color: #7f1d1d;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
  margin-top: 1.25rem;
}

.form-field {
  display: grid;
  align-content: start;
  gap: 0.4rem;
}

.form-field--full-width {
  grid-column: 1 / -1;
}

.form-field label {
  color: #242424;
  font-size: 0.875rem;
  font-weight: 700;
}

.form-field input,
.form-field select {
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.75rem;
  color: #242424;
  background: #ffffff;
  border: 1px solid #a7a7a7;
  border-radius: 0.3rem;
  font: inherit;
}

.form-field input:focus,
.form-field select:focus {
  border-color: #8f001f;
  outline: 0.2rem solid rgb(215 25 63 / 25%);
  outline-offset: 0;
}

.form-field input.ng-invalid.ng-touched,
.form-field select.ng-invalid.ng-touched {
  border-color: #b91c1c;
}

.field-help,
.field-error {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.4;
}

.field-help {
  color: #626262;
}

.field-error {
  color: #b91c1c;
  font-weight: 600;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;
}

.button {
  display: inline-block;
  padding: 0.65rem 0.9rem;
  border: 1px solid #8f001f;
  border-radius: 0.3rem;
  font: inherit;
  font-weight: 700;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
}

.button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.button--primary {
  color: #ffffff;
  background: #8f001f;
}

.button--primary:hover:not(:disabled) {
  background: #650016;
}

.button--secondary {
  color: #8f001f;
  background: #ffffff;
}

.button--secondary:hover:not(:disabled) {
  background: #faf4f5;
}

@media (max-width: 40rem) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .button {
    width: 100%;
    box-sizing: border-box;
  }

  .customer-details__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .customer-details .customer-actions {
    align-items: stretch;
    flex-direction: column;
  }
}

```

