import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  IndividualCustomerAPIService,
  IndividualCustomerViewDto,
} from '../../../../../generated/customer-api';
import { IndividualCustomerContactUpdate } from './individual-customer-contact-update';

describe('IndividualCustomerContactUpdate', () => {
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
  const updateIndividualCustomerContactMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getIndividualCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerContactUpdate],
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
            updateIndividualCustomerContact: updateIndividualCustomerContactMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and populate the form', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);

    // Act
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledWith({ id: customerId });
    expect(component['updateForm'].getRawValue()).toEqual({
      email: 'serafim.gerasimoff@example.com',
      phone: '+359888123456',
    });
  });

  it('should normalize a padded email before validation and submit it', () => {
    // Arrange
    updateIndividualCustomerContactMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].setValue({
      email: '  serafim.new@example.com  ',
      phone: '  +359888000222  ',
    });

    // Act
    component['submit']();

    // Assert
    expect(component['updateForm'].getRawValue()).toEqual({
      email: 'serafim.new@example.com',
      phone: '+359888000222',
    });
    expect(updateIndividualCustomerContactMock).toHaveBeenCalledWith({
      id: customerId,
      customerContactUpdateDto: {
        email: 'serafim.new@example.com',
        phone: '+359888000222',
      },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/individuals', customerId]);
  });

  it('should reject a malformed email without calling Customer Service', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.email.setValue('not-an-email');

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(updateIndividualCustomerContactMock).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Enter a valid email address.');
  });

  it('should reject a blank phone without calling Customer Service', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.phone.setValue('   ');

    // Act
    component['submit']();

    // Assert
    expect(updateIndividualCustomerContactMock).not.toHaveBeenCalled();
    expect(component['updateForm'].controls.phone.touched).toBe(true);
  });

  it('should disable repeat submission while the request is pending', () => {
    // Arrange
    updateIndividualCustomerContactMock.mockReturnValue(new Subject<IndividualCustomerViewDto>());
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(updateIndividualCustomerContactMock).toHaveBeenCalledOnce();
  });

  it('should show a permission message for a forbidden response', () => {
    // Arrange
    updateIndividualCustomerContactMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('You do not have permission to change this Customer.');
    expect(component['submitting']()).toBe(false);
  });

  it('should not offer the form for a closed Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerContactUpdate);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Customer is closed');
    expect(element.querySelector('form')).toBeNull();
  });
});
