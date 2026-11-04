import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityContactUpdate } from './legal-entity-contact-update';

describe('LegalEntityContactUpdate', () => {
  const customerId = '0199e65a-35b1-7000-8000-000000000102';
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
    representatives: [],
  };
  const closedCustomer: LegalEntityCustomerViewDto = {
    ...customer,
    status: LegalEntityCustomerViewDto.StatusEnum.Closed,
  };
  const getLegalEntityCustomerMock = vi.fn();
  const updateLegalEntityCustomerContactMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityContactUpdate],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: customerId }) } },
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
            updateLegalEntityCustomerContact: updateLegalEntityCustomerContactMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });
  it('should load contact details', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityContactUpdate);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance['updateForm'].getRawValue()).toEqual({
      email: customer.email,
      phone: customer.phone,
    });
  });

  it('should normalize and submit contact details', () => {
    // Arrange
    updateLegalEntityCustomerContactMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(LegalEntityContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].setValue({
      email: '  office@starter.example.com  ',
      phone: '  +35921234567  ',
    });

    // Act
    component['submit']();

    // Assert
    expect(updateLegalEntityCustomerContactMock).toHaveBeenCalledWith({
      id: customerId,
      customerContactUpdateDto: { email: 'office@starter.example.com', phone: '+35921234567' },
    });
  });

  it('should reject a malformed email', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.email.setValue('invalid');

    // Act
    component['submit']();

    // Assert
    expect(updateLegalEntityCustomerContactMock).not.toHaveBeenCalled();
  });

  it('should not show the form for a closed Customer', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(LegalEntityContactUpdate);

    // Act
    fixture.detectChanges();

    // Assert
    expect((fixture.nativeElement as HTMLElement).querySelector('form')).toBeNull();
  });

  it('should prevent repeat submission while pending', () => {
    // Arrange
    updateLegalEntityCustomerContactMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(updateLegalEntityCustomerContactMock).toHaveBeenCalledOnce();
  });

  it('should show a permission error', () => {
    // Arrange
    updateLegalEntityCustomerContactMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );
    const fixture = TestBed.createComponent(LegalEntityContactUpdate);
    fixture.detectChanges();

    // Act
    fixture.componentInstance['submit']();
    fixture.detectChanges();

    // Assert
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'You do not have permission to change this Customer.',
    );
  });
});
