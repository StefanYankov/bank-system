import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { eikValidator } from '../../../../../core/forms/eik-validator';
import { notBlankValidator } from '../../../../../core/forms/not-blank-validator';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';
import { customerSchemaConstraints } from '../../../../../generated/customer-schema-constraints.generated';

const constraints = customerSchemaConstraints.EikCorrectionDto.eik;

/**
 * Corrects a wrongly entered EIK for an active Legal Entity Customer.
 */
@Component({
  selector: 'app-legal-entity-eik-correction',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-eik-correction.html',
  styleUrl: './legal-entity-eik-correction.css',
})
export class LegalEntityEikCorrection implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly updateForm = this.formBuilder.group({
    eik: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.minLength(constraints.minLength),
      Validators.maxLength(constraints.maxLength),
      Validators.pattern(constraints.pattern),
      eikValidator(),
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
          this.updateForm.setValue({ eik: customer.eik });
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
    this.updateForm.controls.eik.setValue(this.updateForm.controls.eik.value.trim(), {
      emitEvent: false,
    });
    if (this.updateForm.invalid) {
      this.updateForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.submissionError.set(null);
    this.customerApi
      .correctLegalEntityCustomerEik({
        id: customer.id,
        eikCorrectionDto: this.updateForm.getRawValue(),
      })
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
   * Reports whether the EIK validation message should be displayed.
   *
   * @returns true when the control is invalid and touched
   */
  protected shouldShowError(): boolean {
    const control = this.updateForm.controls.eik;
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
        return 'This EIK is already registered to another Customer, or the Customer is closed.';
      }
    }
    return 'The Legal Entity Customer could not be updated. Please try again.';
  }
}
