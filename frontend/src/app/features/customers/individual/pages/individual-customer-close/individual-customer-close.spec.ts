import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {IndividualCustomerAPIService, IndividualCustomerViewDto} from '../../../../../generated/customer-api';
import {IndividualCustomerClose} from './individual-customer-close';

describe('IndividualCustomerClose', () => {
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
  const closeIndividualCustomerMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getIndividualCustomerMock.mockReturnValue(of(customer));
    navigateMock.mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerClose],
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
            closeIndividualCustomer: closeIndividualCustomerMock,
          },
        },
      ],
    }).compileComponents();

    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and show what is being confirmed', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerClose);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(
      element.querySelector('.customer-maintenance__summary')?.textContent?.replace(/\s+/g, ' ').trim(),
    ).toBe('CUS-000001 · Serafim Ivanov Gerasimoff');
  });

  it('should close the Customer and return to the details page', () => {
    // Arrange
    closeIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerClose);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const confirmButton = element.querySelector('.button--primary') as HTMLButtonElement | null;

    // Act
    confirmButton?.click();

    // Assert
    expect(closeIndividualCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(navigateMock).toHaveBeenCalledWith(['/customers/individuals', customerId]);
  });

  it('should not offer the action when the Customer is already in the target state', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(of(closedCustomer));
    const fixture = TestBed.createComponent(IndividualCustomerClose);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Customer is already closed');
    expect(element.querySelector('.button--primary')).toBeNull();
  });

  it('should disable repeat confirmation while the request is pending', () => {
    // Arrange
    closeIndividualCustomerMock.mockReturnValue(new Subject<IndividualCustomerViewDto>());
    const fixture = TestBed.createComponent(IndividualCustomerClose);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['confirm']();
    component['confirm']();

    // Assert
    expect(closeIndividualCustomerMock).toHaveBeenCalledOnce();
    expect(component['submitting']()).toBe(true);
  });

  it('should show the state message for a conflict', () => {
    // Arrange
    closeIndividualCustomerMock.mockReturnValue(throwError(() => new HttpErrorResponse({status: 409})));
    const fixture = TestBed.createComponent(IndividualCustomerClose);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Act
    component['confirm']();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('The Customer is already closed.');
    expect(component['submitting']()).toBe(false);
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a load error instead of the confirmation', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(throwError(() => new HttpErrorResponse({status: 404})));
    const fixture = TestBed.createComponent(IndividualCustomerClose);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('The requested Individual Customer could not be found.');
    expect(element.querySelector('.button--primary')).toBeNull();
  });
});
