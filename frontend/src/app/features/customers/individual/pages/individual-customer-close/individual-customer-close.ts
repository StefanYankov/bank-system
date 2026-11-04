import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  IndividualCustomerAPIService,
  IndividualCustomerViewDto,
} from '../../../../../generated/customer-api';
import { ReactiveFormsModule } from '@angular/forms';

/**
 * Confirms and closes the relationship with an active Individual Customer.
 */
@Component({
  selector: 'app-individual-customer-close',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './individual-customer-close.html',
  styleUrl: './individual-customer-close.css',
})
export class IndividualCustomerClose implements OnInit {
  protected readonly customer = signal<IndividualCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

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

    this.customerApi
      .getIndividualCustomer({ id: customerId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: IndividualCustomerViewDto) => {
          this.customer.set(customer);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Sends the close request after the user confirmed it.
   */
  protected confirm(): void {
    const customer = this.customer();
    if (customer === null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .closeIndividualCustomer({ id: customer.id })
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
        return 'The Customer is already closed.';
      }
    }

    return 'The Customer could not be closed. Please try again.';
  }
}
