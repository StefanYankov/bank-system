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
