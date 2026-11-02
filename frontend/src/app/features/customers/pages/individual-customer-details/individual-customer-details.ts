import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink} from '@angular/router';

import {IndividualCustomerAPIService, IndividualCustomerViewDto,} from '../../../../generated/customer-api';

/**
 *  Displays the details of one Individual Customer.
 */
@Component({
  selector: 'app-individual-customer-details',
  imports: [RouterLink],
  templateUrl: './individual-customer-details.html',
  styleUrl: './individual-customer-details.css',
})
export class IndividualCustomerDetails implements OnInit {
  protected readonly customer = signal<IndividualCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  private readonly customerApi = inject(IndividualCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /**
   *  Loads the Individual Customer identified by the active route.
   */
  public ngOnInit(): void {
    const customerId = this.route.snapshot.paramMap.get('id');

    if (customerId === null || customerId.length === 0) {
      this.loading.set(false);
      this.error.set('The Customer identifier is missing.');
      return;
    }

    this.loadCustomer(customerId);
  }

  /**
   *  Repeats the request for the current route.
   */
  protected retry(): void {
    const customerId = this.route.snapshot.paramMap.get('id');

    if (customerId === null || customerId.length === 0) {
      return;
    }

    this.loadCustomer(customerId);
  }

  private loadCustomer(customerId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.customer.set(null);

    this.customerApi.getIndividualCustomer({
      id: customerId,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: IndividualCustomerViewDto) => {
          this.customer.set(customer);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(this.errorMessage(error));
          this.loading.set(false);
        },
      });
  }

  private errorMessage(error: unknown): string {
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
}
