import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {catchError, debounceTime, distinctUntilChanged, map, of, startWith, Subject, switchMap, tap} from 'rxjs';
import {AuthSession} from '../../../../core/auth/auth-session';
import {
  CustomerAPIService,
  CustomerSummaryDto,
  PagedResponseCustomerSummaryDto
} from '../../../../generated/customer-api';

interface SearchRequest {
  readonly query: string;
  readonly page: number;
}

type SearchOutcome =
  | {
  readonly kind: 'success';
  readonly response: PagedResponseCustomerSummaryDto;
}
  | {
  readonly kind: 'error';
  readonly error: unknown;
};

/**
 * Searches Customers of both types and displays paginated summary results.
 */
@Component({
  selector: 'app-customer-search',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './customer-search.html',
  styleUrl: './customer-search.css',
})
export class CustomerSearch implements OnInit {
  private static readonly PAGE_SIZE = 10;
  private static readonly SEARCH_DEBOUNCE_MS = 300;

  protected readonly queryControl = new FormControl('', {nonNullable: true});
  private readonly authSession = inject(AuthSession);

  protected readonly customers = signal<readonly CustomerSummaryDto[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly last = signal(true);

  protected readonly canMaintainCustomers = computed(() =>
    this.authSession.hasRealmRole('CLERK')
    || this.authSession.hasRealmRole('ADMIN'),
  );

  private readonly customerApi = inject(CustomerAPIService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<SearchRequest>();
  private currentQuery = '';

  /**
   * Starts the shared request pipeline and debounced query listener.
   */
  public ngOnInit(): void {
    this.requests
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((request) =>
          this.search(request).pipe(
            map((response): SearchOutcome => ({kind: 'success', response})),
            catchError((error: unknown) =>
              of<SearchOutcome>({kind: 'error', error}),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((outcome) => {
        if (outcome.kind === 'success') {
          this.applyPage(outcome.response);
        } else {
          this.customers.set([]);
          this.error.set(this.errorMessage(outcome.error));
        }
        this.loading.set(false);
      });

    this.queryControl.valueChanges
      .pipe(
        startWith(this.queryControl.value),
        map((query) => query.trim()),
        debounceTime(CustomerSearch.SEARCH_DEBOUNCE_MS),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((query) => {
        this.currentQuery = query;
        this.requestPage(0);
      });
  }

  protected clearSearch(): void {
    this.queryControl.setValue('');
  }

  protected previousPage(): void {
    if (!this.loading() && this.page() > 0) {
      this.requestPage(this.page() - 1);
    }
  }

  protected nextPage(): void {
    if (!this.loading() && !this.last()) {
      this.requestPage(this.page() + 1);
    }
  }

  protected retry(): void {
    this.requestPage(this.page());
  }

  /**
   * Builds the router link to the details page for the Customer type.
   *
   * @param customer Customer summary
   * @returns router link commands
   */
  protected detailsLink(customer: CustomerSummaryDto): readonly string[] {
    return customer.customerType === CustomerSummaryDto.CustomerTypeEnum.Individual
      ? ['/customers/individuals', customer.id]
      : ['/customers/legal-entities', customer.id];
  }

  protected customerTypeLabel(
    customerType: CustomerSummaryDto.CustomerTypeEnum,
  ): string {
    return customerType === CustomerSummaryDto.CustomerTypeEnum.Individual
      ? 'Individual'
      : 'Legal entity';
  }

  protected identifierLabel(
    customerType: CustomerSummaryDto.CustomerTypeEnum,
  ): string {
    return customerType === CustomerSummaryDto.CustomerTypeEnum.Individual
      ? 'EGN'
      : 'EIK';
  }

  private requestPage(page: number): void {
    this.requests.next({query: this.currentQuery, page});
  }

  private search(request: SearchRequest) {
    return this.customerApi.searchCustomers({
      ...(request.query.length > 0 ? {query: request.query} : {}),
      page: request.page,
      size: CustomerSearch.PAGE_SIZE,
      sort: ['customerNumber,asc'],
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
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'You do not have permission to view customers.';
    }
    return 'Customers could not be loaded. Please try again.';
  }
}
