import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {IndividualCustomerAPIService, IndividualCustomerViewDto,} from '../../../../generated/customer-api';
import {IndividualCustomerDetails} from './individual-customer-details';

describe('IndividualCustomerDetails', () => {
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
  const getIndividualCustomerMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getIndividualCustomerMock.mockReturnValue(of(customer));

    await TestBed.configureTestingModule({
      imports: [IndividualCustomerDetails],
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
          },
        },
      ],
    }).compileComponents();
  });

  it('should request and render the Individual Customer details', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledOnce();
    expect(getIndividualCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(
      element.querySelector('h1')?.textContent?.replace(/\s+/g, ' ').trim(),
    ).toBe('Serafim Ivanov Gerasimoff');
    expect(element.textContent).toContain('CUS-000001');
    expect(element.textContent).toContain('6101047500');
    expect(element.textContent).toContain('serafim.gerasimoff@example.com');
    expect(element.textContent).toContain('+359888123456');
    expect(element.textContent).toContain('1 Alexander Battenberg Square');
    expect(element.textContent).toContain('Floor 2');
    expect(element.textContent).toContain('1000');
    expect(element.textContent).toContain('Sofia');
    expect(element.textContent).toContain('BG');
    expect(element.textContent).toContain('ACTIVE');
  });

  it('should not display the technical Customer identifier', () => {
    // Arrange
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).not.toContain(customerId);
    expect(element.textContent).not.toContain('Customer ID');
  });

  it('should show a loading state while the request is pending', () => {
    // Arrange
    const response = new Subject<IndividualCustomerViewDto>();
    getIndividualCustomerMock.mockReturnValue(response);
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Loading Individual Customer');
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show a not-found message for a missing Customer', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 404})),
    );
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain('Customer unavailable');
    expect(element.textContent).toContain(
      'The requested Individual Customer could not be found.',
    );
    expect(element.querySelector('.details-card')).toBeNull();
  });

  it('should show an authorization message for a forbidden response', () => {
    // Arrange
    getIndividualCustomerMock.mockReturnValue(
      throwError(() => new HttpErrorResponse({status: 403})),
    );
    const fixture = TestBed.createComponent(IndividualCustomerDetails);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain(
      'You do not have permission to view this Customer.',
    );
  });

  it('should retry loading the Individual Customer', () => {
    // Arrange
    getIndividualCustomerMock
      .mockReturnValueOnce(
        throwError(() => new Error('Customer Service unavailable')),
      )
      .mockReturnValueOnce(of(customer));
    const fixture = TestBed.createComponent(IndividualCustomerDetails);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const retryButton = element.querySelector(
      '.button--primary',
    ) as HTMLButtonElement | null;

    // Act
    retryButton?.click();
    fixture.detectChanges();

    // Assert
    expect(getIndividualCustomerMock).toHaveBeenCalledTimes(2);
    expect(
      element.querySelector('h1')?.textContent?.replace(/\s+/g, ' ').trim(),
    ).toBe('Serafim Ivanov Gerasimoff');
    expect(element.querySelector('.state-panel--error')).toBeNull();
  });
});
