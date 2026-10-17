import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal,} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterLink,} from '@angular/router';

import {EmployeeAPIService, EmployeeViewDto,} from '../../../../generated/employee-api';

/**
 * Reactivates an inactive Employee.
 *
 * The component loads the Employee identified by the active route,
 * verifies the current lifecycle state, requests reactivation through
 * the generated Employee API client, and returns to Employee details.
 */
@Component({
  selector: 'app-employee-reactivate',
  imports: [RouterLink],
  templateUrl: './employee-reactivate.html',
  styleUrl: './employee-reactivate.css',
})
export class EmployeeReactivate implements OnInit {

  protected employeeId: string | null = null;
  protected readonly employee = signal<EmployeeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submissionError = signal<string | null>(null);
  private readonly employeeApi = inject(
    EmployeeAPIService,
  );
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Reads the Employee identifier and loads the current Employee.
   */
  public ngOnInit(): void {
    this.employeeId = this.route.snapshot.paramMap.get('id');

    if (
      this.employeeId === null
      || this.employeeId.length === 0
    ) {
      this.loading.set(false);

      this.loadError.set(
        'The Employee identifier is missing.',
      );

      return;
    }

    this.loadEmployee(this.employeeId);
  }

  /**
   * Submits the Employee reactivation request.
   */
  protected reactivate(): void {
    if (
      this.submitting()
      || this.employeeId === null
      || this.employee()?.status
      === EmployeeViewDto.StatusEnum.Active
    ) {
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    this.employeeApi.reactivateEmployee({
      id: this.employeeId,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (employee: EmployeeViewDto) => {
          void this.router.navigate([
            '/admin/employees',
            employee.id,
          ]);
        },
        error: (error: unknown) => {
          this.submissionError.set(
            this.reactivationErrorMessage(error),
          );

          this.submitting.set(false);
        },
      });
  }

  /**
   * Repeats the initial Employee loading request.
   */
  protected retry(): void {
    if (this.employeeId === null) {
      return;
    }

    this.loadEmployee(this.employeeId);
  }

  /**
   * Loads the Employee required by the reactivation workflow.
   *
   * @param employeeId technical Employee identifier
   */
  private loadEmployee(employeeId: string): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.employee.set(null);

    this.employeeApi.getEmployeeById({
      id: employeeId,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (employee: EmployeeViewDto) => {
          this.employee.set(employee);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(
            this.loadErrorMessage(error),
          );

          this.loading.set(false);
        },
      });
  }

  /**
   * Converts an Employee loading failure into a safe page message.
   *
   * @param error API request failure
   * @returns message displayed by the page
   */
  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Employee could not be found.';
      }

      if (error.status === 403) {
        return 'You do not have permission to reactivate this Employee.';
      }
    }

    return 'The Employee could not be loaded. Please try again.';
  }

  /**
   * Converts an Employee reactivation failure into a safe page message.
   *
   * @param error API request failure
   * @returns message displayed by the page
   */
  private reactivationErrorMessage(
    error: unknown,
  ): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Employee cannot be reactivated in the current state.';
      }

      if (error.status === 404) {
        return 'The Employee no longer exists.';
      }

      if (error.status === 409) {
        return 'The Employee is already active.';
      }

      if (error.status === 403) {
        return 'You do not have permission to reactivate this Employee.';
      }
    }

    return 'The Employee could not be reactivated. Please try again.';
  }
}
