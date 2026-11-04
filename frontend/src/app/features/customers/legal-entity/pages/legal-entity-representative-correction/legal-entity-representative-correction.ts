import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { notBlankValidator } from '../../../../../core/forms/not-blank-validator';
import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeCorrectionDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';
import { customerSchemaConstraints } from '../../../../../generated/customer-schema-constraints.generated';

type NameControl = 'firstName' | 'middleName' | 'lastName';

const constraints = customerSchemaConstraints.RepresentativeCorrectionDto;

/**
 * Corrects the recorded data of a Legal Entity Customer Representative.
 *
 * Linked Representatives keep their Individual Customer link and recorded name
 * snapshot. Unlinked Representative names remain editable. Role and validity
 * dates can be corrected for both types, including removing an incorrect end date.
 */
@Component({
  selector: 'app-legal-entity-representative-correction',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-representative-correction.html',
  styleUrl: './legal-entity-representative-correction.css',
})
export class LegalEntityRepresentativeCorrection implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly representative = signal<RepresentativeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly correctionForm = this.formBuilder.group({
    firstName: this.formBuilder.control('', [
      Validators.maxLength(constraints.firstName.maxLength),
    ]),
    middleName: this.formBuilder.control('', [
      Validators.maxLength(constraints.middleName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [Validators.maxLength(constraints.lastName.maxLength)]),
    role: this.formBuilder.control<RepresentativeCorrectionDto.RoleEnum>(
      RepresentativeCorrectionDto.RoleEnum.LegalRepresentative,
      [Validators.required],
    ),
    validFrom: this.formBuilder.control('', [Validators.required]),
    validTo: this.formBuilder.control(''),
  });
  private readonly customerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    const customerId = this.customerId();
    const representativeId = this.representativeId();

    if (customerId === null || representativeId === null) {
      this.loading.set(false);
      this.loadError.set('The Customer or Representative identifier is missing.');
      return;
    }

    forkJoin({
      customer: this.customerApi.getLegalEntityCustomer({ id: customerId }),
      representative: this.customerApi.getRepresentative({ id: customerId, representativeId }),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ customer, representative }) => {
          this.customer.set(customer);
          this.representative.set(representative);
          this.populateForm(representative);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Reports whether the Representative is linked to an Individual Customer.
   *
   * @returns true when an Individual Customer link is recorded
   */
  protected isLinkedRepresentative(): boolean {
    return this.representative()?.individualCustomerId !== null;
  }

  /**
   * Reports whether a name field should display its validation message.
   *
   * @param controlName name control to inspect
   * @returns true when the control is invalid and touched
   */
  protected shouldShowNameError(controlName: NameControl): boolean {
    const control = this.correctionForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Corrects all recorded Representative values and returns to Customer details.
   */
  protected submit(): void {
    const customer = this.customer();
    const representative = this.representative();

    if (customer === null || representative === null || this.submitting()) {
      return;
    }

    this.normalizeNames();
    this.applyNameValidation(representative);

    if (this.correctionForm.invalid) {
      this.correctionForm.markAllAsTouched();
      return;
    }

    const representativeCorrectionDto = this.createRequest(representative);
    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .correctRepresentative({
        id: customer.id,
        representativeId: representative.id,
        representativeCorrectionDto,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/customers/legal-entities', customer.id]);
        },
        error: (error: unknown) => {
          this.submissionError.set(this.submissionErrorMessage(error));
          this.submitting.set(false);
        },
      });
  }

  /**
   * Returns route commands for the Legal Entity Customer details page.
   *
   * @returns route commands for the Customer details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    if (customerId === null || customerId.length === 0) {
      return null;
    }
    return customerId;
  }

  private representativeId(): string | null {
    const representativeId = this.route.snapshot.paramMap.get('representativeId');
    if (representativeId === null || representativeId.length === 0) {
      return null;
    }
    return representativeId;
  }

  private populateForm(representative: RepresentativeViewDto): void {
    this.correctionForm.setValue({
      firstName: representative.firstName,
      middleName: representative.middleName ?? '',
      lastName: representative.lastName,
      role: representative.role,
      validFrom: representative.validFrom,
      validTo: representative.validTo ?? '',
    });
  }

  private normalizeNames(): void {
    const firstName = this.correctionForm.controls.firstName.value.trim();
    const middleName = this.correctionForm.controls.middleName.value.trim();
    const lastName = this.correctionForm.controls.lastName.value.trim();

    this.correctionForm.patchValue({ firstName, middleName, lastName }, { emitEvent: false });
  }

  private applyNameValidation(representative: RepresentativeViewDto): void {
    const firstName = this.correctionForm.controls.firstName;
    const lastName = this.correctionForm.controls.lastName;

    if (representative.individualCustomerId !== null) {
      firstName.removeValidators([Validators.required, notBlankValidator()]);
      lastName.removeValidators([Validators.required, notBlankValidator()]);
    } else {
      firstName.addValidators([Validators.required, notBlankValidator()]);
      lastName.addValidators([Validators.required, notBlankValidator()]);
    }

    firstName.updateValueAndValidity({ emitEvent: false });
    lastName.updateValueAndValidity({ emitEvent: false });
  }

  private createRequest(representative: RepresentativeViewDto): RepresentativeCorrectionDto {
    const value = this.correctionForm.getRawValue();
    const request: RepresentativeCorrectionDto = {
      role: value.role,
      validFrom: value.validFrom,
    };

    if (representative.individualCustomerId !== null) {
      request.individualCustomerId = representative.individualCustomerId;
    } else {
      request.firstName = value.firstName;
      request.lastName = value.lastName;

      if (value.middleName.length > 0) {
        request.middleName = value.middleName;
      }
    }

    if (value.validTo.length > 0) {
      request.validTo = value.validTo;
    }

    return request;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Representative.';
      }
    }

    return 'The Representative could not be loaded. Please try again.';
  }

  private submissionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) {
        return 'The Representative information is invalid. Review the form and try again.';
      }
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 409) {
        return 'The Representative could not be corrected because the Customer state changed.';
      }
    }

    return 'The Representative could not be corrected. Please try again.';
  }
}
