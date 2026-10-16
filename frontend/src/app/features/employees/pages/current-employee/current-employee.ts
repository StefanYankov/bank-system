import {Component, DestroyRef, inject, OnInit, signal,} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

import {EmployeeAPIService, EmployeeViewDto,} from '../../../../generated/employee-api';

@Component({
  selector: 'app-current-employee',
  templateUrl: './current-employee.html',
  styleUrl: './current-employee.css',
})
export class CurrentEmployee implements OnInit {
  protected readonly loading = signal(true);
  protected readonly employee = signal<EmployeeViewDto | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  private readonly employeeApi = inject(EmployeeAPIService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadCurrentEmployee();
  }

  private loadCurrentEmployee(): void {
    this.loading.set(true);
    this.errorMessage.set(null);


    this.employeeApi
      .getCurrentEmployee()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (employee) => {
          this.employee.set(employee);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.employee.set(null);
          this.errorMessage.set(
            this.getErrorMessage(error),
          );
          this.loading.set(false);
        },
      });
  }

  private getErrorMessage(
    error: HttpErrorResponse,
  ): string {
    if (error.status === 404) {
      return 'No employee profile is linked to this Keycloak account.';
    }

    if (error.status === 403) {
      return 'This account is not allowed to view the employee profile.';
    }

    if (error.status === 0) {
      return 'Employee Service is currently unavailable.';
    }

    return 'The employee profile could not be loaded.';
  }
}
