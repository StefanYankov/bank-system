import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {notBlankValidator} from '../../../../../core/forms/not-blank-validator';
import {
  AddressDto,
  AddressViewDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../../generated/customer-schema-constraints.generated';

const constraints = customerSchemaConstraints.AddressDto;
type AddressControl = 'countryCode' | 'city' | 'postalCode' | 'addressLine1' | 'addressLine2';

/**
 * Replaces the correspondence address of an active Legal Entity Customer.
 */
@Component({
  selector: 'app-legal-entity-correspondence-address-update',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-correspondence-address-update.html',
  styleUrl: './legal-entity-correspondence-address-update.css',
})
export class LegalEntityCorrespondenceAddressUpdate implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly updateForm = this.formBuilder.group({
    countryCode: this.formBuilder.control('', [Validators.required, notBlankValidator(), Validators.pattern(constraints.countryCode.pattern)]),
    city: this.formBuilder.control('', [Validators.required, notBlankValidator(), Validators.maxLength(constraints.city.maxLength)]),
    postalCode: this.formBuilder.control('', [Validators.required, notBlankValidator(), Validators.maxLength(constraints.postalCode.maxLength)]),
    addressLine1: this.formBuilder.control('', [Validators.required, notBlankValidator(), Validators.maxLength(constraints.addressLine1.maxLength)]),
    addressLine2: this.formBuilder.control('', [Validators.maxLength(constraints.addressLine2.maxLength)]),
  });
  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    const id = this.customerId();
    if (id === null) {
      this.loading.set(false);
      this.loadError.set('The Customer identifier is missing.');
      return;
    }

    this.customerApi.getLegalEntityCustomer({id})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          this.customer.set(customer);
          this.populateAddress(customer.correspondenceAddress);
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
    if (customer === null || this.submitting()) return;
    this.normalizeFormValues();
    if (this.updateForm.invalid) {
      this.updateForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.submissionError.set(null);
    this.customerApi.changeLegalEntityCustomerCorrespondenceAddress({id: customer.id, addressDto: this.createRequest()})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => void this.router.navigate(['/customers/legal-entities', updated.id]),
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Reports whether an address-control validation message should be displayed.
   *
   * @param controlName address control name
   * @returns true when the control is invalid and touched
   */
  protected shouldShowError(controlName: AddressControl): boolean {
    const control = this.updateForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Returns the route commands for the current Legal Entity Customer.
   *
   * @returns route commands for the Customer details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private populateAddress(address: AddressViewDto): void {
    this.updateForm.setValue({
      countryCode: address.countryCode,
      city: address.city,
      postalCode: address.postalCode,
      addressLine1: address.addressLine1,
      addressLine2: address.addressLine2 ?? '',
    });
  }

  private normalizeFormValues(): void {
    const value = this.updateForm.getRawValue();
    this.updateForm.setValue({
      countryCode: value.countryCode.trim().toUpperCase(),
      city: value.city.trim(),
      postalCode: value.postalCode.trim(),
      addressLine1: value.addressLine1.trim(),
      addressLine2: value.addressLine2.trim(),
    }, {emitEvent: false});
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
    const id = this.route.snapshot.paramMap.get('id');
    return id !== null && id.length > 0 ? id : null;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Customer.';
      }
    }
    return 'The Legal Entity Customer could not be loaded. Please try again.';
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
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 409) {
        return 'A closed Customer cannot be changed.';
      }
    }
    return 'The Legal Entity Customer could not be updated. Please try again.';
  }
}
