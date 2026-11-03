import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {IndividualCustomerAPIService, IndividualCustomerViewDto} from '../../../../../generated/customer-api';
import {IndividualCustomerNameUpdate} from './individual-customer-name-update';

describe('IndividualCustomerNameUpdate', () => {
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
  const updateIndividualCustomerNameMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getIndividualCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerNameUpdate],
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
            updateIndividualCustomerName: updateIndividualCustomerNameMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and populate the form', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);

    // Act
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(component['updateForm'].getRawValue()).toEqual({
      firstName: 'Serafim',
      middleName: 'Ivanov',
      lastName: 'Gerasimoff',
    });
  });

  it('should normalize, submit, and return to the details page', () => {
    // Arrange
    updateIndividualCustomerNameMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].setValue({
      firstName: '  Serafim  ',
      middleName: '  Petrov  ',
      lastName: '  Gerasimoff  ',
    });

    // Act
    component['submit']();

    // Assert
    expect(component['updateForm'].controls.firstName.value).toBe('Serafim');
    expect(updateIndividualCustomerNameMock).toHaveBeenCalledWith({
      id: customerId,
      individualCustomerNameUpdateDto: {
        firstName: 'Serafim',
        middleName: 'Petrov',
        lastName: 'Gerasimoff',
      },
    });
    expect(navigateMock).toHaveBeenCalledWith(['/customers/individuals', customerId]);
  });

  it('should omit a blank middle name from the request', () => {
    // Arrange
    updateIndividualCustomerNameMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.middleName.setValue('   ');

    // Act
    component['submit']();

    // Assert
    expect(updateIndividualCustomerNameMock).toHaveBeenCalledWith({
      id: customerId,
      individualCustomerNameUpdateDto: {
        firstName: 'Serafim',
        lastName: 'Gerasimoff',
      },
    });
  });

  it('should reject a blank first name without calling Customer Service', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component['updateForm'].controls.firstName.setValue('   ');

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(updateIndividualCustomerNameMock).not.toHaveBeenCalled();
    expect(component['updateForm'].controls.firstName.touched).toBe(true);
    expect(element.textContent).toContain('First name is required');
  });

  it('should disable repeat submission while the request is pending', () => {
    // Arrange
    updateIndividualCustomerNameMock.mockReturnValue(new Subject<IndividualCustomerViewDto>());
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    component['submit']();

    // Assert
    expect(updateIndividualCustomerNameMock).toHaveBeenCalledOnce();
    expect(component['submitting']()).toBe(true);
  });

  it('should show the closed-Customer message for a conflict', () => {
    // Arrange
    updateIndividualCustomerNameMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 409})),
    );
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['submit']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('A closed Customer cannot be changed.');
    expect(component['submitting']()).toBe(false);
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should not offer the form for a closed Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Customer is closed');
    expect(element.querySelector('form')).toBeNull();
  });

  it('should show a load error instead of the form', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 404})),
    );
    const fixture = TestBed.createComponent(IndividualCustomerNameUpdate);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('The requested Individual Customer could not be found.');
    expect(element.querySelector('form')).toBeNull();
  });
});
