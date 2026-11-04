import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
} from '../../../../../generated/customer-api';

/**
 * Confirms and reopens a closed Legal Entity Customer relationship.
 */
@Component({
  selector: 'app-legal-entity-customer-reopen',
  imports: [RouterLink],
  templateUrl: './legal-entity-customer-reopen.html',
  styleUrl: './legal-entity-customer-reopen.css',
})
export class LegalEntityCustomerReopen implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly customerApi = inject(LegalEntityCustomerAPIService);
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
      .getLegalEntityCustomer({ id: customerId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
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
   * Sends the reopen request after the user confirms the operation.
   *
   * Repeated clicks are ignored while the request is pending. On success, the
   * details page is reloaded and displays the new lifecycle state.
   */
  protected confirm(): void {
    const customer = this.customer();
    if (customer === null || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .reopenLegalEntityCustomer({ id: customer.id })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated: LegalEntityCustomerViewDto) => {
          void this.router.navigate(['/customers/legal-entities', updated.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Returns the route commands for the Legal Entity Customer details page.
   *
   * @returns route commands for the details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    return customerId !== null && customerId.length > 0 ? customerId : null;
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
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 409) {
        return 'The Customer is already active.';
      }
    }
    return 'The Customer could not be reopened. Please try again.';
  }
}
