import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { notBlankValidator } from '../../../../core/forms/not-blank-validator';
import {
  EmployeeAPIService,
  EmployeeRegistrationDto,
  EmployeeViewDto,
} from '../../../../generated/employee-api';
import { employeeSchemaConstraints } from '../../../../generated/employee-schema-constraints.generated';

const registrationConstraints = employeeSchemaConstraints.EmployeeRegistrationDto;

/**
 * Registers an Employee linked to an existing Keycloak identity.
 *
 * The component owns the typed registration form, submission state,
 * generated API request, and navigation to the created Employee details page.
 * Form length constraints originate from the Employee Service OpenAPI
 * contract.
 */
@Component({
  selector: 'app-employee-registration',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employee-registration.html',
  styleUrl: './employee-registration.css',
})
export class EmployeeRegistration {
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  /**
   * Typed Employee registration form.
   *
   * Maximum lengths come from generated OpenAPI metadata. The required,
   * email, and not-blank validators remain explicit so the form clearly
   * communicates its validation behaviour.
   */
  protected readonly registrationForm = this.formBuilder.group({
    identitySubjectId: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.identitySubjectId.maxLength),
    ]),
    firstName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.firstName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(registrationConstraints.lastName.maxLength),
    ]),
    workEmail: this.formBuilder.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.email,
      Validators.maxLength(registrationConstraints.workEmail.maxLength),
    ]),
    position: this.formBuilder.control<EmployeeRegistrationDto.PositionEnum>(
      EmployeeRegistrationDto.PositionEnum.Clerk,
      Validators.required,
    ),
    employmentStartDate: this.formBuilder.control('', Validators.required),
  });
  private readonly employeeApi = inject(EmployeeAPIService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Validates and submits the Employee registration form.
   *
   * Invalid controls are marked as touched so the template displays
   * validation feedback. A successful request navigates to the newly
   * registered Employee details page.
   */
  protected submit(): void {
    if (this.registrationForm.invalid || this.submitting()) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submissionError.set(null);

    const formValue = this.registrationForm.getRawValue();

    const request: EmployeeRegistrationDto = {
      identitySubjectId: formValue.identitySubjectId.trim(),
      firstName: formValue.firstName.trim(),
      lastName: formValue.lastName.trim(),
      workEmail: formValue.workEmail.trim(),
      position: formValue.position,
      employmentStartDate: formValue.employmentStartDate,
    };

    this.employeeApi
      .registerEmployee({
        employeeRegistrationDto: request,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (employee: EmployeeViewDto) => {
          void this.router.navigate(['/admin/employees', employee.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.errorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Determines whether a control should display validation feedback.
   *
   * @param controlName registration form control name
   * @returns true when the control is invalid and has been touched
   */
  protected shouldShowError(controlName: keyof typeof this.registrationForm.controls): boolean {
    const control = this.registrationForm.controls[controlName];

    return control.invalid && control.touched;
  }

  /**
   * Converts a registration failure into a safe page message.
   *
   * @param error API request failure
   * @returns message displayed above the registration form
   */
  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Employee information is invalid. Review the form and try again.';
      }

      if (error.status === 409) {
        return 'The Keycloak subject or work email is already linked to an Employee.';
      }

      if (error.status === 403) {
        return 'You do not have permission to register Employees.';
      }
    }

    return 'The Employee could not be registered. Please try again.';
  }
}
