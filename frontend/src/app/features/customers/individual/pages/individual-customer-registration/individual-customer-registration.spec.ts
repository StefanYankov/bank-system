import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {IndividualCustomerAPIService, IndividualCustomerViewDto,} from '../../../../../generated/customer-api';
import {IndividualCustomerRegistration} from './individual-customer-registration';

describe('IndividualCustomerRegistration', () => {
  const registeredCustomer: IndividualCustomerViewDto = {
    id: '0199e65a-35b1-7000-8000-000000000103',
    customerNumber: 'CUS-000003',
    status: IndividualCustomerViewDto.StatusEnum.Active,
    email: 'jane.doe@example.com',
    phone: '+359888000111',
    firstName: 'Jane',
    middleName: null,
    lastName: 'Doe',
    egn: '0041010002',
    residentialAddress: {
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: null,
    },
  };
  const registerMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    registerMock.mockReturnValue(of(registeredCustomer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerRegistration],
      providers: [
        provideRouter([]),
        {
          provide: IndividualCustomerAPIService,
          useValue: {
            registerIndividualCustomer: registerMock,
          },
        },
      ],
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(navigateMock);
  });

  it('should render all registration sections', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Register Individual Customer');
    expect(element.textContent).toContain('Personal details');
    expect(element.textContent).toContain('Contact details');
    expect(element.textContent).toContain('Residential address');
    expect(element.querySelector('#egn')).not.toBeNull();
    expect(element.querySelector('#address-line-1')).not.toBeNull();
  });

  it('should reject an invalid EGN without calling Customer Service', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].patchValue({
      egn: '9999999999',
    });

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].controls.egn.hasError('egn')).toBe(true);
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('should keep an EGN with internal whitespace invalid', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].patchValue({
      egn: '6101 047500',
    });

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].controls.egn.value).toBe('6101 047500');
    expect(component['registrationForm'].controls.egn.invalid).toBe(true);
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('should mark invalid controls as touched after normalization', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].controls.firstName.touched).toBe(true);
    expect(
      component['registrationForm'].controls.residentialAddress.controls.city.touched,
    ).toBe(true);
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('should normalize, validate, and submit the canonical registration', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue({
      email: '  jane.doe@example.com  ',
      phone: '  +359888000111  ',
      firstName: '  Jane  ',
      middleName: '   ',
      lastName: '  Doe  ',
      egn: '  0041010002  ',
      residentialAddress: {
        countryCode: '  bg  ',
        city: '  Sofia  ',
        postalCode: '  1000  ',
        addressLine1: '  1 Alexander Battenberg Square  ',
        addressLine2: '   ',
      },
    });

    // Act
    component['submit']();

    // Assert
    expect(component['registrationForm'].getRawValue()).toEqual({
      email: 'jane.doe@example.com',
      phone: '+359888000111',
      firstName: 'Jane',
      middleName: '',
      lastName: 'Doe',
      egn: '0041010002',
      residentialAddress: {
        countryCode: 'BG',
        city: 'Sofia',
        postalCode: '1000',
        addressLine1: '1 Alexander Battenberg Square',
        addressLine2: '',
      },
    });
    expect(registerMock).toHaveBeenCalledWith({
      individualCustomerRegistrationDto: {
        email: 'jane.doe@example.com',
        phone: '+359888000111',
        firstName: 'Jane',
        lastName: 'Doe',
        egn: '0041010002',
        residentialAddress: {
          countryCode: 'BG',
          city: 'Sofia',
          postalCode: '1000',
          addressLine1: '1 Alexander Battenberg Square',
        },
      },
    });
    expect(navigateMock).toHaveBeenCalledWith([
      '/customers/individuals',
      registeredCustomer.id,
    ]);
  });

  it('should disable repeat submission while the request is pending', () => {
    // Arrange
    const response = new Subject<IndividualCustomerViewDto>();
    registerMock.mockReturnValue(response);
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue({
      email: 'jane.doe@example.com',
      phone: '+359888000111',
      firstName: 'Jane',
      middleName: '',
      lastName: 'Doe',
      egn: '0041010002',
      residentialAddress: {
        countryCode: 'BG',
        city: 'Sofia',
        postalCode: '1000',
        addressLine1: '1 Alexander Battenberg Square',
        addressLine2: '',
      },
    });

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(registerMock).toHaveBeenCalledOnce();
    expect(component['submitting']()).toBe(true);
  });

  it('should show the duplicate-EGN message for a conflict', () => {
    // Arrange
    registerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409})),
    );
    const fixture = TestBed.createComponent(IndividualCustomerRegistration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['registrationForm'].setValue({
      email: 'jane.doe@example.com',
      phone: '+359888000111',
      firstName: 'Jane',
      middleName: '',
      lastName: 'Doe',
      egn: '0041010002',
      residentialAddress: {
        countryCode: 'BG',
        city: 'Sofia',
        postalCode: '1000',
        addressLine1: '1 Alexander Battenberg Square',
        addressLine2: '',
      },
    });

    // Act
    component['submit']();
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.textContent).toContain(
      'This EGN is already registered to another Customer.',
    );
    expect(component['submitting']()).toBe(false);
  });
});
