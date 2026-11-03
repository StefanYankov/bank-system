import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';

import {egnValidator} from '../../../../../core/forms/egn-validator';
import {notBlankValidator} from '../../../../../core/forms/not-blank-validator';
import {
  IndividualCustomerAPIService,
  IndividualCustomerRegistrationDto,
  IndividualCustomerViewDto
} from '../../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../../generated/customer-schema-constraints.generated';

const registrationConstraints =
  customerSchemaConstraints.IndividualCustomerRegistrationDto;
const addressConstraints = customerSchemaConstraints.AddressDto;

type RegistrationControl =
  | 'email'
  | 'phone'
  | 'firstName'
  | 'middleName'
  | 'lastName'
  | 'egn';

type AddressControl =
  | 'countryCode'
  | 'city'
  | 'postalCode'
  | 'addressLine1'
  | 'addressLine2';

/**
 * Registers a new Individual Customer.
 *
 * The typed reactive form uses constraints generated from the Customer Service
 * OpenAPI contract. Harmless presentation differences are normalized before
 * validation, so the displayed, validated, and submitted values are the same.
 * The EGN validator provides immediate birthdate and checksum feedback, while
 * Customer Service remains authoritative for validation, uniqueness,
 * authorization, and persistence.
 */
@Component({
  selector: 'app-individual-customer-registration',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './individual-customer-registration.html',
  styleUrl: './individual-customer-registration.css',
})
export class IndividualCustomerRegistration {
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly registrationForm = this.formBuilder.group({
    email: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.email,
      Validators.maxLength(registrationConstraints.email.maxLength),
    ]),
    phone: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.phone.maxLength),
    ]),
    firstName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.firstName.maxLength),
    ]),
    middleName: this.formBuilder.control('', [
      Validators.maxLength(registrationConstraints.middleName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.lastName.maxLength),
    ]),
    egn: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.minLength(registrationConstraints.egn.minLength),
      Validators.maxLength(registrationConstraints.egn.maxLength),
      Validators.pattern(registrationConstraints.egn.pattern),
      egnValidator(),
    ]),
    residentialAddress: this.formBuilder.group({
      countryCode: this.formBuilder.control('BG', [
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
    }),
  });

  private readonly customerApi = inject(IndividualCustomerAPIService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Normalizes, validates, and submits the registration form.
   *
   * Safe edge whitespace is removed before validation. Internal characters are
   * preserved, so malformed identifiers such as an EGN containing an internal
   * space remain invalid. A successful response navigates to the new Customer
   * details page.
   */
  protected submit(): void {
    if (this.submitting()) {
      return;
    }

    this.normalizeFormValues();

    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);
    const request = this.createRegistrationRequest();

    this.customerApi.registerIndividualCustomer({
      individualCustomerRegistrationDto: request,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: IndividualCustomerViewDto) => {
          void this.router.navigate([
            '/customers/individuals',
            customer.id,
          ]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.errorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Reports whether a top-level registration control should display an error.
   *
   * @param controlName registration control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowError(controlName: RegistrationControl): boolean {
    const control = this.registrationForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Reports whether an address control should display an error.
   *
   * @param controlName residential-address control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowAddressError(controlName: AddressControl): boolean {
    const control =
      this.registrationForm.controls.residentialAddress.controls[controlName];
    return control.invalid && control.touched;
  }

  private normalizeFormValues(): void {
    const value = this.registrationForm.getRawValue();

    this.registrationForm.patchValue({
      email: value.email.trim(),
      phone: value.phone.trim(),
      firstName: value.firstName.trim(),
      middleName: value.middleName.trim(),
      lastName: value.lastName.trim(),
      egn: value.egn.trim(),
      residentialAddress: {
        countryCode: value.residentialAddress.countryCode.trim().toUpperCase(),
        city: value.residentialAddress.city.trim(),
        postalCode: value.residentialAddress.postalCode.trim(),
        addressLine1: value.residentialAddress.addressLine1.trim(),
        addressLine2: value.residentialAddress.addressLine2.trim(),
      },
    }, {
      emitEvent: false,
    });
  }

  private createRegistrationRequest(): IndividualCustomerRegistrationDto {
    const value = this.registrationForm.getRawValue();

    return {
      email: value.email,
      phone: value.phone,
      firstName: value.firstName,
      ...(value.middleName.length > 0
        ? {middleName: value.middleName}
        : {}),
      lastName: value.lastName,
      egn: value.egn,
      residentialAddress: {
        countryCode: value.residentialAddress.countryCode,
        city: value.residentialAddress.city,
        postalCode: value.residentialAddress.postalCode,
        addressLine1: value.residentialAddress.addressLine1,
        ...(value.residentialAddress.addressLine2.length > 0
          ? {addressLine2: value.residentialAddress.addressLine2}
          : {}),
      },
    };
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Customer information is invalid. Review the form and try again.';
      }
      if (error.status === 409) {
        return 'This EGN is already registered to another Customer.';
      }
      if (error.status === 403) {
        return 'You do not have permission to register Customers.';
      }
    }

    return 'The Individual Customer could not be registered. Please try again.';
  }
}
