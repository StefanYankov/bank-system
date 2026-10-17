import {Component, DestroyRef, inject, signal,} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {EmployeeAPIService, EmployeeViewDto, PagedResponseEmployeeViewDto,} from '../../../../generated/employee-api';

@Component({
  selector: 'app-employee-list',
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.css',
})
export class EmployeeList {
  private static readonly PAGE_SIZE = 10;
  protected readonly employees = signal<readonly EmployeeViewDto[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly last = signal(true);
  private readonly employeeApi = inject(EmployeeAPIService);
  private readonly destroyRef = inject(DestroyRef);

  public constructor() {
    this.loadPage(0);
  }

  protected previousPage(): void {
    if (this.loading() || this.page() === 0) {
      return;
    }

    this.loadPage(this.page() - 1);
  }

  protected nextPage(): void {
    if (this.loading() || this.last()) {
      return;
    }

    this.loadPage(this.page() + 1);
  }

  protected retry(): void {
    this.loadPage(this.page());
  }

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

  private applyPage(
    response: PagedResponseEmployeeViewDto,
  ): void {
    this.employees.set(response.content);
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
      return 'You do not have permission to view employees.';
    }

    return 'Employees could not be loaded. Please try again.';
  }
}
