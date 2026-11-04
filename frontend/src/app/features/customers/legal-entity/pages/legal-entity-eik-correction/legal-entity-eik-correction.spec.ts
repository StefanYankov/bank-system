import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';
import { LegalEntityEikCorrection } from './legal-entity-eik-correction';

describe('LegalEntityEikCorrection', () => {
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
  const correctLegalEntityCustomerEikMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityEikCorrection],
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
            correctLegalEntityCustomerEik: correctLegalEntityCustomerEikMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });
  it('should load the current EIK', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityEikCorrection);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance['updateForm'].controls.eik.value).toBe('123456786');
  });

  it('should trim and submit a valid EIK', () => {
    // Arrange
    correctLegalEntityCustomerEikMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(LegalEntityEikCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.eik.setValue('  100000086  ');

    // Act
    component['submit']();

    // Assert
    expect(correctLegalEntityCustomerEikMock).toHaveBeenCalledWith({
      id: customerId,
      eikCorrectionDto: { eik: '100000086' },
    });
  });

  it('should reject a wrong check digit', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityEikCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.eik.setValue('123456789');

    // Act
    component['submit']();

    // Assert
    expect(correctLegalEntityCustomerEikMock).not.toHaveBeenCalled();
  });

  it('should keep internal whitespace invalid', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityEikCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.eik.setValue('1234 56786');

    // Act
    component['submit']();

    // Assert
    expect(component['updateForm'].controls.eik.value).toBe('1234 56786');
    expect(correctLegalEntityCustomerEikMock).not.toHaveBeenCalled();
  });

  it('should not show the form for a closed Customer', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(LegalEntityEikCorrection);

    // Act
    fixture.detectChanges();

    // Assert
    expect((fixture.nativeElement as HTMLElement).querySelector('form')).toBeNull();
  });

  it('should show the duplicate or closed message for a conflict', () => {
    // Arrange
    correctLegalEntityCustomerEikMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409 })),
    );
    const fixture = TestBed.createComponent(LegalEntityEikCorrection);
    fixture.detectChanges();

    // Act
    fixture.componentInstance['submit']();
    fixture.detectChanges();

    // Assert
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'This EIK is already registered to another Customer',
    );
  });
});
