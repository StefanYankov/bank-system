import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityCustomerRegistration } from './legal-entity-customer-registration';

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
    component['registrationForm'].setValue({ ...validFormValue, eik: '123456789' });

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
    component['registrationForm'].setValue({ ...validFormValue, eik: '1234 56786' });

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
    expect(navigateMock).toHaveBeenCalledWith(['/customers/legal-entities', registeredCustomer.id]);
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
    registerMock.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
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
