import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {notBlankValidator} from '../../../../../core/forms/not-blank-validator';
import {
  IndividualCustomerAPIService,
  IndividualCustomerNameUpdateDto,
  IndividualCustomerViewDto,
} from '../../../../../generated/customer-api';
import {customerSchemaConstraints} from '../../../../../generated/customer-schema-constraints.generated';

const nameConstraints = customerSchemaConstraints.IndividualCustomerNameUpdateDto;

type NameControl = 'firstName' | 'middleName' | 'lastName';

/**
 * Updates the recorded name of an active Individual Customer.
 */
@Component({
  selector: 'app-individual-customer-name-update',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './individual-customer-name-update.html',
  styleUrl: './individual-customer-name-update.css',
})
export class IndividualCustomerNameUpdate implements OnInit {
  protected readonly customer = signal<IndividualCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly updateForm = this.formBuilder.group({
    firstName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(nameConstraints.firstName.maxLength),
    ]),
    middleName: this.formBuilder.control('', [
      Validators.maxLength(nameConstraints.middleName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(nameConstraints.lastName.maxLength),
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
            firstName: customer.firstName,
            middleName: customer.middleName ?? '',
            lastName: customer.lastName,
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

    this.customerApi.updateIndividualCustomerName({
      id: customer.id,
      individualCustomerNameUpdateDto: this.createRequest(),
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
  protected shouldShowError(controlName: NameControl): boolean {
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
      firstName: value.firstName.trim(),
      middleName: value.middleName.trim(),
      lastName: value.lastName.trim(),
    }, {
      emitEvent: false,
    });
  }

  private createRequest(): IndividualCustomerNameUpdateDto {
    const value = this.updateForm.getRawValue();
    return {
      firstName: value.firstName,
      ...(value.middleName.length > 0 ? {middleName: value.middleName} : {}),
      lastName: value.lastName,
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

    return 'The Customer name could not be updated. Please try again.';
  }
}
