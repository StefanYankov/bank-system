import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {egnValidator} from '../../../../core/forms/egn-validator';
import {notBlankValidator} from '../../../../core/forms/not-blank-validator';
import {IndividualCustomerAPIService, IndividualCustomerViewDto} from '../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../generated/customer-schema-constraints.generated';

const egnConstraints = customerSchemaConstraints.EgnCorrectionDto.egn;

/**
 * Corrects an EGN that was entered incorrectly for an active Individual Customer.
 */
@Component({
  selector: 'app-individual-customer-egn-correction',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './individual-customer-egn-correction.html',
  styleUrl: './individual-customer-egn-correction.css',
})
export class IndividualCustomerEgnCorrection implements OnInit {
  protected readonly customer = signal<IndividualCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly updateForm = this.formBuilder.group({
    egn: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.minLength(egnConstraints.minLength),
      Validators.maxLength(egnConstraints.maxLength),
      Validators.pattern(egnConstraints.pattern),
      egnValidator(),
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
            egn: customer.egn,
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

    this.updateForm.controls.egn.setValue(
      this.updateForm.controls.egn.value.trim(),
      {emitEvent: false},
    );

    if (this.updateForm.invalid) {
      this.updateForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi.correctIndividualCustomerEgn({
      id: customer.id,
      egnCorrectionDto: {
        egn: this.updateForm.controls.egn.value,
      },
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
   * Reports whether the EGN control should display its validation message.
   *
   * @returns true when the EGN is invalid and has been touched
   */
  protected shouldShowError(): boolean {
    const control = this.updateForm.controls.egn;
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
        return 'This EGN is already registered to another Customer, or the Customer is closed.';
      }
    }

    return 'The EGN could not be corrected. Please try again.';
  }
}
