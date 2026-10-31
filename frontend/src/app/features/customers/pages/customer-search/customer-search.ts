import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {debounceTime, distinctUntilChanged, startWith} from 'rxjs';

import {
  CustomerAPIService,
  CustomerSummaryDto,
  PagedResponseCustomerSummaryDto,
} from '../../../../generated/customer-api';

/**
 * Searches Customers of both types and displays paginated summary results.
 */
@Component({
  selector: 'app-customer-search',
  imports: [ReactiveFormsModule],
  templateUrl: './customer-search.html',
  styleUrl: './customer-search.css',
})
export class CustomerSearch implements OnInit {
  private static readonly PAGE_SIZE = 10;
  private static readonly SEARCH_DEBOUNCE_MS = 300;

  protected readonly queryControl = new FormControl('', {
    nonNullable: true,
  });
  protected readonly customers = signal<readonly CustomerSummaryDto[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly last = signal(true);

  private readonly customerApi = inject(CustomerAPIService);
  private readonly destroyRef = inject(DestroyRef);
  private currentQuery = '';

  /**
   * Starts the initial load and listens for debounced query changes.
   */
  public ngOnInit(): void {
    this.queryControl.valueChanges
      .pipe(
        startWith(this.queryControl.value),
        debounceTime(CustomerSearch.SEARCH_DEBOUNCE_MS),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((query) => {
        this.currentQuery = query.trim();
        this.loadPage(0);
      });
  }

  /**
   * Clears the search query. The value-change subscription reloads page zero.
   */
  protected clearSearch(): void {
    this.queryControl.setValue('');
  }

  /**
   * Loads the previous result page when one is available.
   */
  protected previousPage(): void {
    if (this.loading() || this.page() === 0) {
      return;
    }

    this.loadPage(this.page() - 1);
  }

  /**
   * Loads the next result page when one is available.
   */
  protected nextPage(): void {
    if (this.loading() || this.last()) {
      return;
    }

    this.loadPage(this.page() + 1);
  }

  /**
   * Repeats the request for the currently selected page and query.
   */
  protected retry(): void {
    this.loadPage(this.page());
  }

  /**
   * Returns a user-facing label for a Customer type.
   *
   * @param customerType generated API Customer type
   * @returns display label
   */
  protected customerTypeLabel(
    customerType: CustomerSummaryDto.CustomerTypeEnum,
  ): string {
    return customerType === CustomerSummaryDto.CustomerTypeEnum.Individual
      ? 'Individual'
      : 'Legal entity';
  }

  /**
   * Returns the identifier label appropriate for the Customer type.
   *
   * @param customerType generated API Customer type
   * @returns EGN or EIK
   */
  protected identifierLabel(
    customerType: CustomerSummaryDto.CustomerTypeEnum,
  ): string {
    return customerType === CustomerSummaryDto.CustomerTypeEnum.Individual
      ? 'EGN'
      : 'EIK';
  }

  private loadPage(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.customerApi.searchCustomers({
      ...(this.currentQuery.length > 0
        ? {query: this.currentQuery}
        : {}),
      page,
      size: CustomerSearch.PAGE_SIZE,
      sort: ['customerNumber,asc'],
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response: PagedResponseCustomerSummaryDto) => {
          this.applyPage(response);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.customers.set([]);
          this.error.set(this.errorMessage(error));
          this.loading.set(false);
        },
      });
  }

  private applyPage(response: PagedResponseCustomerSummaryDto): void {
    this.customers.set(response.content);
    this.page.set(response.page);
    this.totalElements.set(response.totalElements);
    this.totalPages.set(response.totalPages);
    this.last.set(response.last);
  }

  private errorMessage(error: unknown): string {
    if (
      error instanceof HttpErrorResponse
      && error.status === 403
    ) {
      return 'You do not have permission to view customers.';
    }

    return 'Customers could not be loaded. Please try again.';
  }
}
