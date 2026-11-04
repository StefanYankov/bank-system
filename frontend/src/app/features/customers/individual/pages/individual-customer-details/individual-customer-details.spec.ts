import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthSession } from '../../../../../core/auth/auth-session';
import {
  IndividualCustomerAPIService,
  IndividualCustomerViewDto,
} from '../../../../../generated/customer-api';
import { IndividualCustomerDetails } from './individual-customer-details';

describe('IndividualCustomerDetails', () => {
  const customerId = '0199e65a-35b1-7000-8000-000000000101';
  const customer: IndividualCustomerViewDto = {
    id: customerId,
    customerNumber: 'CUS-000001',
    status: IndividualCustomerViewDto.StatusEnum.Active,
    email: 'serafim.gerasimoff@example.com',
    phone: '+359888123456',
    firstName: 'Serafim',
    middleName: 'Ivanov',
    lastName: 'Gerasimoff',
    egn: '6101047500',
    residentialAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: 'Floor 2',
    },
  };
  const closedCustomer: IndividualCustomerViewDto = {
    ...customer,
    status: IndividualCustomerViewDto.StatusEnum.Closed,
  };
  const getIndividualCustomerMock = vi.fn();
  let realmRoles: readonly string[] = [];

  beforeEach(async () => {
    vi.clearAllMocks();
    realmRoles = ['CLERK'];
    getIndividualCustomerMock.mockReturnValue(of(customer));

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerDetails],
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
          provide: IndividualCustomerAPIService,
          useValue: {
            getIndividualCustomer: getIndividualCustomerMock,
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

  it('should request and render the Individual Customer details', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledOnce();
    expect(getIndividualCustomerMock).toHaveBeenCalledWith({ id: customerId });
    expect(element.querySelector('h1')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Serafim Ivanov Gerasimoff',
    );
    expect(element.textContent).toContain('CUS-000001');
    expect(element.textContent).toContain('6101047500');
    expect(element.textContent).toContain('serafim.gerasimoff@example.com');
    expect(element.textContent).toContain('+359888123456');
    expect(element.textContent).toContain('1 Alexander Battenberg Square');
    expect(element.textContent).toContain('Floor 2');
    expect(element.textContent).toContain('Sofia');
    expect(element.textContent).toContain('ACTIVE');
  });

  it('should not display the technical Customer identifier as text', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).not.toContain(customerId);
  });

  it('should show maintenance and close actions to a clerk for an active Customer', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector('#update-name-action')?.getAttribute('href')).toBe(
      `/customers/individuals/${customerId}/name`,
    );
    expect(element.querySelector('#update-contact-action')?.getAttribute('href')).toBe(
      `/customers/individuals/${customerId}/contact`,
    );
    expect(element.querySelector('#update-address-action')?.getAttribute('href')).toBe(
      `/customers/individuals/${customerId}/residential-address`,
    );
    expect(element.querySelector('#correct-egn-action')?.getAttribute('href')).toBe(
      `/customers/individuals/${customerId}/egn`,
    );
    expect(element.querySelector('#close-action')?.getAttribute('href')).toBe(
      `/customers/individuals/${customerId}/close`,
    );
    expect(element.querySelector('#reopen-action')).toBeNull();
  });

  it('should show maintenance actions to an administrator', () => {
    // Arrange
    realmRoles = ['ADMIN'];
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector('.customer-actions')).not.toBeNull();
  });

  it('should show only reopening for a closed Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector('#reopen-action')?.getAttribute('href')).toBe(
      `/customers/individuals/${customerId}/reopen`,
    );
    expect(element.querySelector('#update-name-action')).toBeNull();
    expect(element.querySelector('#close-action')).toBeNull();
  });

  it('should hide every action from a loan officer', () => {
    // Arrange
    realmRoles = ['LOAN_OFFICER'];
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector('.customer-actions')).toBeNull();
    expect(element.textContent).toContain('CUS-000001');
  });

  it('should show a loading state while the request is pending', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(new Subject<IndividualCustomerViewDto>());
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Loading Individual Customer');
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show a not-found message for a missing Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Customer unavailable');
    expect(element.textContent).toContain('The requested Individual Customer could not be found.');
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show an authorization message for a forbidden response', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('You do not have permission to view this Customer.');
  });

  it('should retry loading the Individual Customer', () => {
    // Arrange
    getIndividualCustomerMock
      .mockReturnValueOnce(throwError(() => new Error('Customer Service unavailable')))
      .mockReturnValueOnce(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const retryButton = element.querySelector('.state-panel .button') as HTMLButtonElement | null;

    // Act
    retryButton?.click();
    fixture.detectChanges();

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledTimes(2);
    expect(element.querySelector('h1')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Serafim Ivanov Gerasimoff',
    );
    expect(element.querySelector('.state-panel--error')).toBeNull();
  });
});
