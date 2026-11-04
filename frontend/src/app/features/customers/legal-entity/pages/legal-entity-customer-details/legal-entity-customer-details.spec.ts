import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {AuthSession} from '../../../../../core/auth/auth-session';
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

  it('should show maintenance actions to a clerk for an active Customer', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Update company name');
    expect(element.textContent).toContain('Update contact');
    expect(element.textContent).toContain('Update registered address');
    expect(element.textContent).toContain('Update correspondence address');
    expect(element.textContent).toContain('Correct EIK');
  });

  it('should show maintenance actions to an administrator', () => {
    realmRoles = ['ADMIN'];
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.customer-actions')).not.toBeNull();
  });

  it('should hide maintenance actions from a loan officer', () => {
    realmRoles = ['LOAN_OFFICER'];
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.customer-actions')).toBeNull();
    expect(element.textContent).toContain('Starter Ltd');
  });

  it('should hide maintenance actions for a closed Customer', () => {
    getLegalEntityCustomerMock.mockReturnValue(of({
      ...customer,
      status: LegalEntityCustomerViewDto.StatusEnum.Closed,
    }));
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.customer-actions')).toBeNull();
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
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain(customerId);
  });

  it('should show a loading state while the request is pending', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

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
