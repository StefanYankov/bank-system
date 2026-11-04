import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {LegalEntityCustomerAPIService, LegalEntityCustomerViewDto} from '../../../../../generated/customer-api';
import {LegalEntityCompanyNameUpdate} from './legal-entity-company-name-update';

describe('LegalEntityCompanyNameUpdate', () => {
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
  const updateLegalEntityCustomerCompanyNameMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [LegalEntityCompanyNameUpdate],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {paramMap: convertToParamMap({id: customerId})}},
        },
        {
          provide: LegalEntityCustomerAPIService,
          useValue: {
            getLegalEntityCustomer: getLegalEntityCustomerMock,
            updateLegalEntityCustomerCompanyName: updateLegalEntityCustomerCompanyNameMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });
  it('should load the current company name', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCompanyNameUpdate);

    // Act
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance['updateForm'].controls.companyName.value).toBe('Starter Ltd');
  });

  it('should normalize, submit, and navigate to details', () => {
    // Arrange
    updateLegalEntityCustomerCompanyNameMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(LegalEntityCompanyNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.companyName.setValue('  Starter Financial Services  ');

    // Act
    component['submit']();

    // Assert
    expect(updateLegalEntityCustomerCompanyNameMock).toHaveBeenCalledWith({
      id: customerId,
      companyNameUpdateDto: {companyName: 'Starter Financial Services'},
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/legal-entities', customerId]);
  });

  it('should reject a blank company name', () => {
    // Arrange
    const fixture = TestBed.createComponent(LegalEntityCompanyNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.companyName.setValue('   ');

    // Act
    component['submit']();

    // Assert
    expect(updateLegalEntityCustomerCompanyNameMock).not.toHaveBeenCalled();
    expect(component['updateForm'].controls.companyName.touched).toBe(true);
  });

  it('should not show the form for a closed Customer', () => {
    // Arrange
    getLegalEntityCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(LegalEntityCompanyNameUpdate);

    // Act
    fixture.detectChanges();

    // Assert
    expect((fixture.nativeElement as HTMLElement).querySelector('form')).toBeNull();
  });

  it('should prevent repeat submission while pending', () => {
    // Arrange
    updateLegalEntityCustomerCompanyNameMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityCompanyNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(updateLegalEntityCustomerCompanyNameMock).toHaveBeenCalledOnce();
  });

  it('should show a conflict message', () => {
    // Arrange
    updateLegalEntityCustomerCompanyNameMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409})),
    );
    const fixture = TestBed.createComponent(LegalEntityCompanyNameUpdate);
    fixture.detectChanges();

    // Act
    fixture.componentInstance['submit']();
    fixture.detectChanges();

    // Assert
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('A closed Customer cannot be changed.');
  });
});
