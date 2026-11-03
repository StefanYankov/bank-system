import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {IndividualCustomerAPIService, IndividualCustomerViewDto} from '../../../../generated/customer-api';
import {IndividualCustomerAddressUpdate} from './individual-customer-address-update';

describe('IndividualCustomerAddressUpdate', () => {
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
  const changeIndividualCustomerResidentialAddressMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getIndividualCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerAddressUpdate],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({id: customerId}),
            },
          },
        },
        {
          provide: IndividualCustomerAPIService,
          useValue: {
            getIndividualCustomer: getIndividualCustomerMock,
            changeIndividualCustomerResidentialAddress: changeIndividualCustomerResidentialAddressMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and populate the address', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);

    // Act
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Assert
    expect(component['updateForm'].getRawValue()).toEqual({
      countryCode: 'BG',
      city: 'Sofia',
      postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square',
      addressLine2: 'Floor 2',
    });
  });

  it('should normalize, submit, and return to the details page', () => {
    // Arrange
    changeIndividualCustomerResidentialAddressMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].setValue({
      countryCode: '  bg  ',
      city: '  Troyan  ',
      postalCode: '  5600  ',
      addressLine1: '  41 Rakovski Street  ',
      addressLine2: '   ',
    });

    // Act
    component['submit']();

    // Assert
    expect(component['updateForm'].controls.countryCode.value).toBe('BG');
    expect(changeIndividualCustomerResidentialAddressMock).toHaveBeenCalledWith({
      id: customerId,
      addressDto: {
        countryCode: 'BG',
        city: 'Troyan',
        postalCode: '5600',
        addressLine1: '41 Rakovski Street',
      },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/individuals', customerId]);
  });

  it('should keep internal spacing in address lines', () => {
    // Arrange
    changeIndividualCustomerResidentialAddressMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.addressLine2.setValue('Block 2,  entrance A');

    // Act
    component['submit']();

    // Assert
    expect(changeIndividualCustomerResidentialAddressMock).toHaveBeenCalledWith({
      id: customerId,
      addressDto: {
        countryCode: 'BG',
        city: 'Sofia',
        postalCode: '1000',
        addressLine1: '1 Alexander Battenberg Square',
        addressLine2: 'Block 2,  entrance A',
      },
    });
  });

  it('should reject an invalid country code without calling Customer Service', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.countryCode.setValue('B1');

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(changeIndividualCustomerResidentialAddressMock).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Enter a two-letter country code.');
  });

  it('should show the closed-Customer message for a conflict', () => {
    // Arrange
    changeIndividualCustomerResidentialAddressMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409})),
    );
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('A closed Customer cannot be changed.');
    expect(component['submitting']()).toBe(false);
  });

  it('should not offer the form for a closed Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.querySelector('form')).toBeNull();
  });

  it('should disable repeat submission while the request is pending', () => {
    // Arrange
    changeIndividualCustomerResidentialAddressMock.mockReturnValue(new Subject<IndividualCustomerViewDto>());
    const fixture = TestBed.createComponent(IndividualCustomerAddressUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(changeIndividualCustomerResidentialAddressMock).toHaveBeenCalledOnce();
  });
});
