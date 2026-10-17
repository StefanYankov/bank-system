import {HttpErrorResponse} from '@angular/common/http';
import {Component, DestroyRef, inject, OnInit, signal,} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators,} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink,} from '@angular/router';

import {EmployeeAPIService, EmployeePositionChangeDto, EmployeeViewDto,} from '../../../../generated/employee-api';

/**
 * Changes the position assigned to an existing Employee.
 *
 * The component loads the Employee identified by the active route,
 * initializes a typed position form, sends the generated position-change
 * request, and returns to the updated Employee details page.
 */
@Component({
  selector: 'app-employee-position-change',
  imports: [
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './employee-position-change.html',
  styleUrl: './employee-position-change.css',
})
export class EmployeePositionChange implements OnInit {

  protected employeeId: string | null = null;
  protected readonly employee = signal<EmployeeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submissionError = signal<string | null>(null);
  protected readonly positions = [
    {
      value: EmployeePositionChangeDto.PositionEnum.Clerk,
      label: 'Clerk',
    },
    {
      value:
      EmployeePositionChangeDto.PositionEnum.LoanOfficer,
      label: 'Loan officer',
    },
    {
      value:
      EmployeePositionChangeDto.PositionEnum.Administrator,
      label: 'Administrator',
    },
  ] as const;
  private readonly formBuilder = inject(NonNullableFormBuilder,);
  /**
   * Typed Employee position form.
   */
  protected readonly positionForm = this.formBuilder.group({
    position:
      this.formBuilder.control<EmployeePositionChangeDto.PositionEnum>(
        EmployeePositionChangeDto.PositionEnum.Clerk,
        Validators.required,
      ),
  });
  private readonly employeeApi = inject(EmployeeAPIService,);
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
   * Validates and submits the Employee position change.
   */
  protected submit(): void {
    if (
      this.positionForm.invalid
      || this.submitting()
      || this.employeeId === null
    ) {
      this.positionForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    const request: EmployeePositionChangeDto = {
      position:
      this.positionForm.controls.position.value,
    };

    this.employeeApi.changeEmployeePosition({
      id: this.employeeId,
      employeePositionChangeDto: request,
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
            this.updateErrorMessage(error),
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
   * Loads the Employee and selects the current position.
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

          this.positionForm.setValue({
            position:
              employee.position as EmployeePositionChangeDto.PositionEnum,
          });

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
        return 'You do not have permission to change this Employee position.';
      }
    }

    return 'The Employee could not be loaded. Please try again.';
  }

  /**
   * Converts a position-change failure into a safe page message.
   *
   * @param error API request failure
   * @returns message displayed above the form
   */
  private updateErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The selected Employee position is invalid.';
      }

      if (error.status === 404) {
        return 'The Employee no longer exists.';
      }

      if (error.status === 403) {
        return 'You do not have permission to change this Employee position.';
      }
    }

    return 'The Employee position could not be changed. Please try again.';
  }
}
