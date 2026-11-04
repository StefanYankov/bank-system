import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { notBlankValidator } from '../../../../core/forms/not-blank-validator';
import {
  EmployeeAPIService,
  EmployeeUpdateDto,
  EmployeeViewDto,
} from '../../../../generated/employee-api';
import { employeeSchemaConstraints } from '../../../../generated/employee-schema-constraints.generated';

/**
 * Edits the profile information of an existing Employee.
 *
 * The component loads the Employee identified by the active route,
 * initializes a typed profile form, sends the generated update request,
 * and navigates back to the updated Employee details page.
 */
@Component({
  selector: 'app-employee-profile-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employee-profile-edit.html',
  styleUrl: './employee-profile-edit.css',
})
export class EmployeeProfileEdit implements OnInit {
  protected employeeId: string | null = null;
  protected readonly updateConstraints = employeeSchemaConstraints.EmployeeUpdateDto;
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submissionError = signal<string | null>(null);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  /**
   * Typed Employee profile form.
   *
   * Maximum lengths come from the generated Employee Service OpenAPI
   * constraints. Required, email, and not-blank behavior remains explicit.
   */
  protected readonly profileForm = this.formBuilder.group({
    firstName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(this.updateConstraints.firstName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(this.updateConstraints.lastName.maxLength),
    ]),
    workEmail: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.email,
      Validators.maxLength(this.updateConstraints.workEmail.maxLength),
    ]),
  });
  private readonly employeeApi = inject(EmployeeAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Reads the Employee identifier and loads the current profile.
   */
  public ngOnInit(): void {
    this.employeeId = this.route.snapshot.paramMap.get('id');

    if (this.employeeId === null || this.employeeId.length === 0) {
      this.loading.set(false);
      this.loadError.set('The Employee identifier is missing.');
      return;
    }

    this.loadEmployee(this.employeeId);
  }

  /**
   * Validates and submits the updated Employee profile.
   */
  protected submit(): void {
    if (this.profileForm.invalid || this.submitting() || this.employeeId === null) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    const formValue = this.profileForm.getRawValue();

    const request: EmployeeUpdateDto = {
      firstName: formValue.firstName.trim(),
      lastName: formValue.lastName.trim(),
      workEmail: formValue.workEmail.trim(),
    };

    this.employeeApi
      .updateEmployeeProfile({
        id: this.employeeId,
        employeeUpdateDto: request,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (employee: EmployeeViewDto) => {
          void this.router.navigate(['/admin/employees', employee.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.updateErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Repeats the Employee loading request.
   */
  protected retry(): void {
    if (this.employeeId === null) {
      return;
    }

    this.loadEmployee(this.employeeId);
  }

  /**
   * Determines whether a control should display validation feedback.
   *
   * @param controlName profile form control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowError(controlName: keyof typeof this.profileForm.controls): boolean {
    const control = this.profileForm.controls[controlName];

    return control.invalid && control.touched;
  }

  /**
   * Loads the Employee and initializes the editable profile fields.
   *
   * @param employeeId technical Employee identifier
   */
  private loadEmployee(employeeId: string): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.employeeApi
      .getEmployeeById({
        id: employeeId,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (employee: EmployeeViewDto) => {
          this.profileForm.setValue({
            firstName: employee.firstName,
            lastName: employee.lastName,
            workEmail: employee.workEmail,
          });

          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
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
        return 'You do not have permission to edit this Employee.';
      }
    }

    return 'The Employee could not be loaded. Please try again.';
  }

  /**
   * Converts an Employee update failure into a safe page message.
   *
   * @param error API request failure
   * @returns message displayed above the form
   */
  private updateErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Employee information is invalid. Review the form and try again.';
      }

      if (error.status === 404) {
        return 'The Employee no longer exists.';
      }

      if (error.status === 409) {
        return 'The work email is already assigned to another Employee.';
      }

      if (error.status === 403) {
        return 'You do not have permission to edit this Employee.';
      }
    }

    return 'The Employee profile could not be updated. Please try again.';
  }
}
