import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {notBlankValidator} from '../../../../core/forms/not-blank-validator';
import {AddressDto, IndividualCustomerAPIService, IndividualCustomerViewDto} from '../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../generated/customer-schema-constraints.generated';

const addressConstraints = customerSchemaConstraints.AddressDto;

type AddressControl = 'countryCode' | 'city' | 'postalCode' | 'addressLine1' | 'addressLine2';

/**
 * Replaces the residential address of an active Individual Customer.
 */
@Component({
  selector: 'app-individual-customer-address-update',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './individual-customer-address-update.html',
  styleUrl: './individual-customer-address-update.css',
})
export class IndividualCustomerAddressUpdate implements OnInit {
  protected readonly customer = signal<IndividualCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly updateForm = this.formBuilder.group({
    countryCode: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.pattern(addressConstraints.countryCode.pattern),
    ]),
    city: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(addressConstraints.city.maxLength),
    ]),
    postalCode: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(addressConstraints.postalCode.maxLength),
    ]),
    addressLine1: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(addressConstraints.addressLine1.maxLength),
    ]),
    addressLine2: this.formBuilder.control('', [
      Validators.maxLength(addressConstraints.addressLine2.maxLength),
    ]),
  });

  private readonly customerApi = inject(IndividualCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      this.loading.set(false);
      this.loadError.set('The Customer identifier is missing.');
      return;
    }

    this.customerApi.getIndividualCustomer({id: customerId})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: IndividualCustomerViewDto) => {
          this.customer.set(customer);
          this.updateForm.setValue({
            countryCode: customer.residentialAddress.countryCode,
            city: customer.residentialAddress.city,
            postalCode: customer.residentialAddress.postalCode,
            addressLine1: customer.residentialAddress.addressLine1,
            addressLine2: customer.residentialAddress.addressLine2 ?? '',
          });
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  protected submit(): void {
    const customer = this.customer();
    if (customer === null || this.submitting()) {
      return;
    }

    this.normalizeFormValues();

    if (this.updateForm.invalid) {
      this.updateForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi.changeIndividualCustomerResidentialAddress({
      id: customer.id,
      addressDto: this.createRequest(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated: IndividualCustomerViewDto) => {
          void this.router.navigate(['/customers/individuals', updated.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Reports whether a form control should display its validation message.
   *
   * @param controlName form control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowError(controlName: AddressControl): boolean {
    const control = this.updateForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Router commands for the Customer details page.
   *
   * @returns router-link commands for the details page
   */
  protected customerLink(): string[] {
    return ['/customers/individuals', this.customerId() ?? ''];
  }

  private normalizeFormValues(): void {
    const value = this.updateForm.getRawValue();
    this.updateForm.setValue({
      countryCode: value.countryCode.trim().toUpperCase(),
      city: value.city.trim(),
      postalCode: value.postalCode.trim(),
      addressLine1: value.addressLine1.trim(),
      addressLine2: value.addressLine2.trim(),
    }, {
      emitEvent: false,
    });
  }

  private createRequest(): AddressDto {
    const value = this.updateForm.getRawValue();
    return {
      countryCode: value.countryCode,
      city: value.city,
      postalCode: value.postalCode,
      addressLine1: value.addressLine1,
      ...(value.addressLine2.length > 0 ? {addressLine2: value.addressLine2} : {}),
    };
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    return customerId !== null && customerId.length > 0 ? customerId : null;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Individual Customer could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Customer.';
      }
    }

    return 'The Individual Customer could not be loaded. Please try again.';
  }

  private submissionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The submitted information is invalid. Review the form and try again.';
      }
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The requested Individual Customer could not be found.';
      }
      if (error.status === 409) {
        return 'A closed Customer cannot be changed.';
      }
    }

    return 'The residential address could not be updated. Please try again.';
  }
}
