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
    // Arrange
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

    // Act
    component['searchCustomers']();
    fixture.detectChanges();

    // Assert
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
    // Arrange
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

    // Act
    component['submit']();

    // Assert
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
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityRepresentativeAddition);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['changeMode']('UNLINKED');
    component['representativeForm'].controls.validFrom.setValue('2026-11-04');

    // Act
    component['submit']();

    // Assert
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
