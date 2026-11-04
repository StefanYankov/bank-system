import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthSession } from '../../../../../core/auth/auth-session';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';

/**
 * Displays the details of one Legal Entity Customer.
 */
@Component({
  selector: 'app-legal-entity-customer-details',
  imports: [RouterLink],
  templateUrl: './legal-entity-customer-details.html',
  styleUrl: './legal-entity-customer-details.css',
})
export class LegalEntityCustomerDetails implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly today = localIsoDate(new Date());
  private readonly authSession = inject(AuthSession);

  protected readonly canMaintainCustomers =
    this.authSession.hasRealmRole('CLERK') || this.authSession.hasRealmRole('ADMIN');

  public ngOnInit(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      this.loading.set(false);
      this.error.set('The Customer identifier is missing.');
      return;
    }
    this.loadCustomer(customerId);
  }

  /**
   * Repeats the request for the Customer identified by the current route.
   */
  protected retry(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      return;
    }
    this.loadCustomer(customerId);
  }

  /**
   * Builds the displayed full name of a Representative.
   *
   * @param representative Representative to display
   * @returns first, optional middle, and last name separated by spaces
   */
  protected representativeName(representative: RepresentativeViewDto): string {
    return [representative.firstName, representative.middleName, representative.lastName]
      .filter((part): part is string => part !== null && part.length > 0)
      .join(' ');
  }

  /**
   * Converts a Representative role to its display label.
   *
   * @param role Representative role
   * @returns display label
   */
  protected roleLabel(role: RepresentativeViewDto.RoleEnum): string {
    if (role === RepresentativeViewDto.RoleEnum.LegalRepresentative) {
      return 'Legal representative';
    }
    return 'Authorized representative';
  }

  /**
   * Reports whether a Representative's authority has ended.
   *
   * @param representative Representative to check
   * @returns true when the inclusive valid-to date is before today
   */
  protected hasEnded(representative: RepresentativeViewDto): boolean {
    return representative.validTo !== null && representative.validTo < this.today;
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    if (customerId === null || customerId.length === 0) {
      return null;
    }
    return customerId;
  }

  private loadCustomer(customerId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.customer.set(null);
    this.customerApi
      .getLegalEntityCustomer({ id: customerId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
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
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Customer.';
      }
    }
    return 'The Legal Entity Customer could not be loaded. Please try again.';
  }
}

/**
 * Formats a date as a local calendar date without converting it to UTC.
 *
 * The returned format matches the ISO date-only values used by Customer
 * Service, allowing safe chronological comparison of `YYYY-MM-DD` strings.
 *
 * @param date date whose local calendar fields should be formatted
 * @returns local date in `YYYY-MM-DD` format
 */
function localIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
