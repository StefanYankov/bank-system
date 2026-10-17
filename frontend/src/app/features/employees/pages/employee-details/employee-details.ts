import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {EmployeeAPIService, EmployeeViewDto} from "../../../../generated/employee-api";
import {ActivatedRoute, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';

/**
 * Displays the administrative details of one Employee.
 *
 * The Employee identifier is read from the active route and passed to the
 * generated Employee API client. The component owns only page-level loading,
 * success, and error state.
 */
@Component({
  imports: [RouterLink],
  selector: 'app-employee-details',
  styleUrl: './employee-details.css',
  templateUrl: './employee-details.html',
})
export class EmployeeDetails implements OnInit {
  protected readonly employee = signal<EmployeeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private readonly employeeApi = inject(EmployeeAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Loads the Employee identified by the current route.
   */
  public ngOnInit(): void {
    const employeeId = this.route.snapshot.paramMap.get('id');

    if (employeeId === null || employeeId.length === 0) {
      this.loading.set(false);
      this.error.set('The Employee identifier is missing.');
      return;
    }

    this.loadEmployee(employeeId);
  }

  /**
   * Repeats the Employee request for the current route.
   */
  protected retry(): void {
    const employeeId = this.route.snapshot.paramMap.get('id');

    if (employeeId === null || employeeId.length === 0) {
      return;
    }

    this.loadEmployee(employeeId);
  }

  /**
   * Retrieves one Employee and updates the page state.
   *
   * @param employeeId technical Employee identifier from the route
   */
  private loadEmployee(employeeId: string): void {
    this.loading.set(true);
    this.error.set(null);
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
          this.error.set(this.errorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Converts an HTTP failure into a client-safe page message.
   *
   * @param error API request failure
   * @return message displayed by the page
   */
  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Employee could not be found.';
      }

      if (error.status === 403) {
        return 'You do not have permission to view this Employee.';
      }
    }

    return 'The Employee could not be loaded. Please try again.';
  }
}
