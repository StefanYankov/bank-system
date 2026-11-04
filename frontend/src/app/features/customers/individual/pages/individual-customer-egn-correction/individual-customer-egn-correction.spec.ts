import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  IndividualCustomerAPIService,
  IndividualCustomerViewDto,
} from '../../../../../generated/customer-api';
import { IndividualCustomerEgnCorrection } from './individual-customer-egn-correction';

describe('IndividualCustomerEgnCorrection', () => {
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
  const correctIndividualCustomerEgnMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getIndividualCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerEgnCorrection],
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
            correctIndividualCustomerEgn: correctIndividualCustomerEgnMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and pre-fill the current EGN', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);

    // Act
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Assert
    expect(component['updateForm'].controls.egn.value).toBe('6101047500');
  });

  it('should trim, submit, and return to the details page', () => {
    // Arrange
    correctIndividualCustomerEgnMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.egn.setValue('  0041010002  ');

    // Act
    component['submit']();

    // Assert
    expect(correctIndividualCustomerEgnMock).toHaveBeenCalledWith({
      id: customerId,
      egnCorrectionDto: { egn: '0041010002' },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/individuals', customerId]);
  });

  it('should reject an EGN with an invalid check digit', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.egn.setValue('6101047501');

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(correctIndividualCustomerEgnMock).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Enter a valid 10-digit EGN');
  });

  it('should keep an EGN with internal whitespace invalid', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.egn.setValue('6101 047500');

    // Act
    component['submit']();

    // Assert
    expect(component['updateForm'].controls.egn.value).toBe('6101 047500');
    expect(correctIndividualCustomerEgnMock).not.toHaveBeenCalled();
  });

  it('should show the duplicate message for a conflict', () => {
    // Arrange
    correctIndividualCustomerEgnMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409 })),
    );
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.egn.setValue('0041010002');

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('This EGN is already registered to another Customer');
    expect(component['submitting']()).toBe(false);
  });

  it('should not offer the form for a closed Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector('form')).toBeNull();
  });

  it('should disable repeat submission while the request is pending', () => {
    // Arrange
    correctIndividualCustomerEgnMock.mockReturnValue(new Subject<IndividualCustomerViewDto>());
    const fixture = TestBed.createComponent(IndividualCustomerEgnCorrection);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(correctIndividualCustomerEgnMock).toHaveBeenCalledOnce();
  });
});
