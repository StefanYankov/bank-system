import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { notBlankValidator } from '../../../../../core/forms/not-blank-validator';
import {
  CustomerContactUpdateDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';
import { customerSchemaConstraints } from '../../../../../generated/customer-schema-constraints.generated';

const constraints = customerSchemaConstraints.CustomerContactUpdateDto;
type ContactControl = 'email' | 'phone';

/**
 * Updates the email and phone of an active Legal Entity Customer.
 *
 * Safe edge whitespace is normalized before validation. Customer Service
 * remains authoritative for authorization and lifecycle rules.
 */
@Component({
  selector: 'app-legal-entity-contact-update',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-contact-update.html',
  styleUrl: './legal-entity-contact-update.css',
})
export class LegalEntityContactUpdate implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly updateForm = this.formBuilder.group({
    email: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.email,
      Validators.maxLength(constraints.email.maxLength),
    ]),
    phone: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(constraints.phone.maxLength),
    ]),
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

    this.customerApi
      .getLegalEntityCustomer({ id })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          this.customer.set(customer);
          this.updateForm.setValue({ email: customer.email, phone: customer.phone });
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

    const value = this.updateForm.getRawValue();
    this.updateForm.setValue(
      { email: value.email.trim(), phone: value.phone.trim() },
      { emitEvent: false },
    );

    if (this.updateForm.invalid) {
      this.updateForm.markAllAsTouched();
      return;
    }

    const request: CustomerContactUpdateDto = this.updateForm.getRawValue();
    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .updateLegalEntityCustomerContact({ id: customer.id, customerContactUpdateDto: request })
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
   * Reports whether a contact-control validation message should be displayed.
   *
   * @param controlName contact control name
   * @returns true when the control is invalid and touched
   */
  protected shouldShowError(controlName: ContactControl): boolean {
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
