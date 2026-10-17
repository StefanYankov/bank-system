import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal,} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {EmployeeAPIService, EmployeeViewDto, PagedResponseEmployeeViewDto,} from '../../../../generated/employee-api';
import {RouterLink} from '@angular/router';

/**
 * Displays a paginated administrative list of Employees.
 *
 * The component retrieves Employee records through the generated OpenAPI
 * client and owns the loading, success, empty, error, and pagination states
 * required by the page.
 */
@Component({
  selector: 'app-employee-list',
  imports: [RouterLink],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.css',
})
export class EmployeeList implements OnInit {

  private static readonly PAGE_SIZE = 10;
  private readonly employeeApi = inject(EmployeeAPIService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly employees = signal<readonly EmployeeViewDto[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly last = signal(true);

  /**
   * Loads the first Employee page when Angular initialises the component.
   */
  public ngOnInit(): void {
    this.loadPage(0);
  }

  /**
   * Loads the previous Employee page when one is available.
   */
  protected previousPage(): void {
    if (this.loading() || this.page() === 0) {
      return;
    }

    this.loadPage(this.page() - 1);
  }

  /**
   * Loads the next Employee page when one is available.
   */
  protected nextPage(): void {
    if (this.loading() || this.last()) {
      return;
    }

    this.loadPage(this.page() + 1);
  }

  /**
   * Repeats the request for the currently selected page.
   */
  protected retry(): void {
    this.loadPage(this.page());
  }

  /**
   * Retrieves one page of Employees and updates the displayed page state.
   *
   * @param page zero-based page number to retrieve
   */
  private loadPage(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.employeeApi.getEmployees({
      page,
      size: EmployeeList.PAGE_SIZE,
      sort: ['employeeNumber,asc'],
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response: PagedResponseEmployeeViewDto) => {
          this.applyPage(response);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.employees.set([]);
          this.error.set(this.errorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Copies a successful paged response into the component state.
   *
   * @param response paginated Employee response returned by Employee Service
   */
  private applyPage(
    response: PagedResponseEmployeeViewDto,
  ): void {
    this.employees.set(response.content);
    this.page.set(response.page);
    this.totalElements.set(response.totalElements);
    this.totalPages.set(response.totalPages);
    this.last.set(response.last);
  }

  /**
   * Converts an Employee list request failure into a safe page message.
   *
   * Detailed RFC 9457 handling remains outside this PR. The current
   * implementation retains page-specific wording and a safe fallback.
   *
   * @param error API request failure
   * @return message displayed by the Employee list page
   */
  private errorMessage(error: unknown): string {
    if (
      error instanceof HttpErrorResponse
      && error.status === 403
    ) {
      return 'You do not have permission to view employees.';
    }

    return 'Employees could not be loaded. Please try again.';
  }
}
