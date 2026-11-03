import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {IndividualCustomerAPIService, IndividualCustomerViewDto} from '../../../../../generated/customer-api';
import {ReactiveFormsModule} from '@angular/forms';

/**
 * Confirms and reopens the relationship with a closed Individual Customer.
 *
 * Reopening keeps the same Customer number and identifiers, so existing
 * records stay linked to one Customer. The current Customer is loaded first,
 * so the user confirms against the right name and number, and a Customer that
 * is already active is reported instead of offering the action.
 */
@Component({
  selector: 'app-individual-customer-reopen',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './individual-customer-reopen.html',
  styleUrl: './individual-customer-reopen.css',
})
export class IndividualCustomerReopen implements OnInit {
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

    this.customerApi.getIndividualCustomer({id: customerId})
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
   * Sends the reopen request after the user confirmed it.
   *
   * Repeated clicks are ignored while the request is pending. A successful
   * response returns to the Customer details page, which then shows the new
   * status and its available actions.
   */
  protected confirm(): void {
    const customer = this.customer();
    if (customer === null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi.reopenIndividualCustomer({id: customer.id})
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
   * Uses the route identifier, so the back and cancel links work even while
   * the Customer is still loading or failed to load.
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
        return 'The Customer is already active.';
      }
    }

    return 'The Customer could not be reopened. Please try again.';
  }
}
