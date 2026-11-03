import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {eikValidator} from '../../../../../core/forms/eik-validator';
import {notBlankValidator} from '../../../../../core/forms/not-blank-validator';
import {
  AddressDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerRegistrationDto,
  LegalEntityCustomerViewDto
} from '../../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../../generated/customer-schema-constraints.generated';

const registrationConstraints =
  customerSchemaConstraints.LegalEntityCustomerRegistrationDto;
const addressConstraints = customerSchemaConstraints.AddressDto;

type RegistrationControl =
  | 'email'
  | 'phone'
  | 'companyName'
  | 'eik';

type AddressGroup =
  | 'registeredAddress'
  | 'correspondenceAddress';

type AddressControl =
  | 'countryCode'
  | 'city'
  | 'postalCode'
  | 'addressLine1'
  | 'addressLine2';

interface AddressFormValue {
  readonly countryCode: string;
  readonly city: string;
  readonly postalCode: string;
  readonly addressLine1: string;
  readonly addressLine2: string;
}

/**
 * Registers a new Legal Entity Customer.
 *
 * The typed reactive form uses constraints generated from the Customer Service
 * OpenAPI contract and collects the registered and correspondence addresses.
 * Harmless edge whitespace is removed before validation. The EIK validator
 * provides immediate check-digit feedback, while Customer Service remains
 * authoritative for validation, uniqueness, authorization, and persistence.
 * Representatives are added separately after registration.
 */
@Component({
  selector: 'app-legal-entity-customer-registration',
  imports: [ReactiveFormsModule],
  templateUrl: './legal-entity-customer-registration.html',
  styleUrl: './legal-entity-customer-registration.css',
})
export class LegalEntityCustomerRegistration {
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
    companyName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.companyName.maxLength),
    ]),
    eik: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.minLength(registrationConstraints.eik.minLength),
      Validators.maxLength(registrationConstraints.eik.maxLength),
      Validators.pattern(registrationConstraints.eik.pattern),
      eikValidator(),
    ]),
    registeredAddress: this.createAddressGroup(),
    correspondenceAddress: this.createAddressGroup(),
  });

  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Normalizes, validates, and submits the registration form.
   *
   * Safe edge whitespace is removed before validation. Internal characters are
   * preserved, so an EIK containing an internal space remains invalid. A
   * successful response navigates to the new Customer details page.
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

    this.customerApi.registerLegalEntityCustomer({
      legalEntityCustomerRegistrationDto: this.createRegistrationRequest(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          void this.router.navigate([
            '/customers/legal-entities',
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
   * @param groupName address group name
   * @param controlName address control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowAddressError(groupName: AddressGroup, controlName: AddressControl): boolean {
    const control = this.registrationForm.controls[groupName].controls[controlName];
    return control.invalid && control.touched;
  }

  private createAddressGroup() {
    return this.formBuilder.group({
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
    });
  }

  private normalizeFormValues(): void {
    const value = this.registrationForm.getRawValue();

    this.registrationForm.patchValue({
      email: value.email.trim(),
      phone: value.phone.trim(),
      companyName: value.companyName.trim(),
      eik: value.eik.trim(),
      registeredAddress: this.normalizeAddress(value.registeredAddress),
      correspondenceAddress: this.normalizeAddress(value.correspondenceAddress),
    }, {
      emitEvent: false,
    });
  }

  private normalizeAddress(address: AddressFormValue): AddressFormValue {
    return {
      countryCode: address.countryCode.trim().toUpperCase(),
      city: address.city.trim(),
      postalCode: address.postalCode.trim(),
      addressLine1: address.addressLine1.trim(),
      addressLine2: address.addressLine2.trim(),
    };
  }

  private createRegistrationRequest(): LegalEntityCustomerRegistrationDto {
    const value = this.registrationForm.getRawValue();

    return {
      email: value.email,
      phone: value.phone,
      companyName: value.companyName,
      eik: value.eik,
      registeredAddress: this.toAddressDto(value.registeredAddress),
      correspondenceAddress: this.toAddressDto(value.correspondenceAddress),
    };
  }

  private toAddressDto(address: AddressFormValue): AddressDto {
    return {
      countryCode: address.countryCode,
      city: address.city,
      postalCode: address.postalCode,
      addressLine1: address.addressLine1,
      ...(address.addressLine2.length > 0
        ? {addressLine2: address.addressLine2}
        : {}),
    };
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Customer information is invalid. Review the form and try again.';
      }
      if (error.status === 409) {
        return 'This EIK is already registered to another Customer.';
      }
      if (error.status === 403) {
        return 'You do not have permission to register Customers.';
      }
    }
    return 'The Legal Entity Customer could not be registered. Please try again.';
  }
}
