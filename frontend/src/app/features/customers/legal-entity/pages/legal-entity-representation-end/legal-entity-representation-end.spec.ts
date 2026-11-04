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
