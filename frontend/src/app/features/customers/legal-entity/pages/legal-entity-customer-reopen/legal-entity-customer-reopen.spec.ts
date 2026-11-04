import {HttpErrorResponse} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {LegalEntityCustomerAPIService, LegalEntityCustomerViewDto} from '../../../../../generated/customer-api';
import {LegalEntityCustomerReopen} from './legal-entity-customer-reopen';

describe('LegalEntityCustomerReopen', () => {
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
      countryCode: 'BG', city: 'Sofia', postalCode: '1000',
      addressLine1: '1 Alexander Battenberg Square', addressLine2: 'Floor 3',
    },
    correspondenceAddress: {
      countryCode: 'BG', city: 'Plovdiv', postalCode: '4000',
      addressLine1: '10 Main Street', addressLine2: null,
    },
    representatives: [],
  };
  const closedCustomer: LegalEntityCustomerViewDto = {
    ...customer,
    status: LegalEntityCustomerViewDto.StatusEnum.Closed,
  };
  const getLegalEntityCustomerMock = vi.fn();
  const reopenLegalEntityCustomerMock = vi.fn();
  const navigateMock = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();
    getLegalEntityCustomerMock.mockReturnValue(of(closedCustomer));
    navigateMock.mockResolvedValue(true);
    await TestBed.configureTestingModule({
      imports: [LegalEntityCustomerReopen],
      providers: [
        provideRouter([]),
        {provide: ActivatedRoute, useValue: {snapshot: {paramMap: convertToParamMap({id: customerId})}}},
        {provide: LegalEntityCustomerAPIService, useValue: {getLegalEntityCustomer: getLegalEntityCustomerMock, reopenLegalEntityCustomer: reopenLegalEntityCustomerMock}},
      ],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigateMock);
  });

  it('should load the Customer and show what is being confirmed', () => {
    const fixture = TestBed.createComponent(LegalEntityCustomerReopen);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(getLegalEntityCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(element.querySelector('.customer-maintenance__summary')?.textContent?.replace(/\s+/g, ' ').trim())
      .toBe('CUS-000002 · Starter Ltd');
  });

  it('should reopen the Customer and return to details', () => {
    reopenLegalEntityCustomerMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(LegalEntityCustomerReopen);
    fixture.detectChanges();
    fixture.componentInstance['confirm']();
    expect(reopenLegalEntityCustomerMock).toHaveBeenCalledWith({id: customerId});
    expect(navigateMock).toHaveBeenCalledWith(['/customers/legal-entities', customerId]);
  });

  it('should not offer the action when already active', () => {
    getLegalEntityCustomerMock.mockReturnValue(of(customer));
    const fixture = TestBed.createComponent(LegalEntityCustomerReopen);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Customer is already active');
    expect((fixture.nativeElement as HTMLElement).querySelector('.button--primary')).toBeNull();
  });

  it('should prevent repeat confirmation while pending', () => {
    reopenLegalEntityCustomerMock.mockReturnValue(new Subject<LegalEntityCustomerViewDto>());
    const fixture = TestBed.createComponent(LegalEntityCustomerReopen);
    fixture.detectChanges();
    fixture.componentInstance['confirm']();
    fixture.componentInstance['confirm']();
    expect(reopenLegalEntityCustomerMock).toHaveBeenCalledOnce();
  });

  it('should show the state message for a conflict', () => {
    reopenLegalEntityCustomerMock.mockReturnValue(throwError(() => new HttpErrorResponse({status: 409})));
    const fixture = TestBed.createComponent(LegalEntityCustomerReopen);
    fixture.detectChanges();
    fixture.componentInstance['confirm']();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('The Customer is already active.');
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('should show a load error instead of confirmation', () => {
    getLegalEntityCustomerMock.mockReturnValue(throwError(() => new HttpErrorResponse({status: 404})));
    const fixture = TestBed.createComponent(LegalEntityCustomerReopen);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('The requested Legal Entity Customer could not be found.');
  });
});
