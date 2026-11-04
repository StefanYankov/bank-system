import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { notBlankValidator } from '../../../../../core/forms/not-blank-validator';
import {
  CustomerAPIService,
  CustomerSummaryDto,
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentativeAdditionDto,
} from '../../../../../generated/customer-api';
import { customerSchemaConstraints } from '../../../../../generated/customer-schema-constraints.generated';

type RepresentativeMode = 'LINKED' | 'UNLINKED';
type NameControl = 'firstName' | 'middleName' | 'lastName';

const representativeConstraints = customerSchemaConstraints.RepresentativeAdditionDto;

/**
 * Adds a linked or unlinked Representative to an active Legal Entity Customer.
 */
@Component({
  selector: 'app-legal-entity-representative-addition',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-representative-addition.html',
  styleUrl: './legal-entity-representative-addition.css',
})
export class LegalEntityRepresentativeAddition implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly searchResults = signal<readonly CustomerSummaryDto[]>([]);
  protected readonly searching = signal(false);
  protected readonly searchError = signal<string | null>(null);
  protected readonly searchPerformed = signal(false);
  protected readonly selectedCustomer = signal<CustomerSummaryDto | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly representativeForm = this.formBuilder.group({
    mode: this.formBuilder.control<RepresentativeMode>('LINKED'),
    firstName: this.formBuilder.control('', [
      Validators.maxLength(representativeConstraints.firstName.maxLength),
    ]),
    middleName: this.formBuilder.control('', [
      Validators.maxLength(representativeConstraints.middleName.maxLength),
    ]),
    lastName: this.formBuilder.control('', [
      Validators.maxLength(representativeConstraints.lastName.maxLength),
    ]),
    role: this.formBuilder.control<RepresentativeAdditionDto.RoleEnum>(
      RepresentativeAdditionDto.RoleEnum.LegalRepresentative,
      [Validators.required],
    ),
    validFrom: this.formBuilder.control('', [Validators.required]),
  });
  protected readonly searchForm = this.formBuilder.group({
    query: this.formBuilder.control('', [Validators.required, notBlankValidator()]),
  });
  private readonly customerApi = inject(CustomerAPIService);
  private readonly legalEntityCustomerApi = inject(LegalEntityCustomerAPIService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  public ngOnInit(): void {
    const customerId = this.customerId();
    if (customerId === null) {
      this.loading.set(false);
      this.loadError.set('The Customer identifier is missing.');
      return;
    }

    this.legalEntityCustomerApi
      .getLegalEntityCustomer({ id: customerId })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (customer: LegalEntityCustomerViewDto) => {
          this.customer.set(customer);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Returns whether the linked-Representative mode is selected.
   *
   * @returns true when the Representative will be linked to an Individual Customer
   */
  protected isLinkedMode(): boolean {
    return this.representativeForm.controls.mode.value === 'LINKED';
  }

  /**
   * Changes the Representative source and clears values belonging to the other mode.
   *
   * @param mode selected Representative source
   */
  protected changeMode(mode: RepresentativeMode): void {
    this.representativeForm.controls.mode.setValue(mode);
    this.submissionError.set(null);

    if (mode === 'LINKED') {
      this.clearNames();
      return;
    }

    this.selectedCustomer.set(null);
    this.searchResults.set([]);
    this.searchError.set(null);
    this.searchPerformed.set(false);
  }

  /**
   * Searches for active Individual Customers that can be linked as Representatives.
   */
  protected searchCustomers(): void {
    this.normalizeSearchQuery();
    if (this.searchForm.invalid || this.searching()) {
      this.searchForm.markAllAsTouched();
      return;
    }

    const query = this.searchForm.controls.query.value;
    this.searching.set(true);
    this.searchError.set(null);
    this.searchPerformed.set(true);
    this.searchResults.set([]);

    this.customerApi
      .searchCustomers({ query, page: 0, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const eligibleCustomers = page.content.filter((candidate) => {
            const isIndividual =
              candidate.customerType === CustomerSummaryDto.CustomerTypeEnum.Individual;
            const isActive = candidate.status === CustomerSummaryDto.StatusEnum.Active;
            return isIndividual && isActive;
          });

          this.searchResults.set(eligibleCustomers);
          this.searching.set(false);
        },
        error: () => {
          this.searchError.set('Individual Customers could not be searched. Please try again.');
          this.searching.set(false);
        },
      });
  }

  /**
   * Selects an active Individual Customer as the linked Representative.
   *
   * @param customer selected Individual Customer summary
   */
  protected selectCustomer(customer: CustomerSummaryDto): void {
    this.selectedCustomer.set(customer);
    this.searchResults.set([]);
    this.searchError.set(null);
  }

  /**
   * Clears the selected Individual Customer and returns to linked-Customer search.
   */
  protected clearSelectedCustomer(): void {
    this.selectedCustomer.set(null);
    this.searchPerformed.set(false);
  }

  /**
   * Reports whether a name field should display its validation message.
   *
   * @param controlName name control to inspect
   * @returns true when the control is invalid and touched
   */
  protected shouldShowNameError(controlName: NameControl): boolean {
    const control = this.representativeForm.controls[controlName];
    return control.invalid && control.touched;
  }

  /**
   * Adds the Representative using the selected source and form values.
   */
  protected submit(): void {
    const customer = this.customer();
    if (customer === null || this.submitting()) {
      return;
    }

    this.normalizeNames();
    this.applyModeValidation();

    if (!this.hasValidRepresentativeSource() || this.representativeForm.invalid) {
      this.representativeForm.markAllAsTouched();
      return;
    }

    const representativeAdditionDto = this.createRequest();
    this.submitting.set(true);
    this.submissionError.set(null);

    this.legalEntityCustomerApi
      .addRepresentative({ id: customer.id, representativeAdditionDto })
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
   * Returns route commands for the current Legal Entity Customer details page.
   *
   * @returns route commands for the Customer details page
   */
  protected customerLink(): string[] {
    return ['/customers/legal-entities', this.customerId() ?? ''];
  }

  private customerId(): string | null {
    const customerId = this.route.snapshot.paramMap.get('id');
    return customerId !== null && customerId.length > 0 ? customerId : null;
  }

  private normalizeSearchQuery(): void {
    const query = this.searchForm.controls.query.value.trim();
    this.searchForm.controls.query.setValue(query, { emitEvent: false });
  }

  private normalizeNames(): void {
    const firstName = this.representativeForm.controls.firstName.value.trim();
    const middleName = this.representativeForm.controls.middleName.value.trim();
    const lastName = this.representativeForm.controls.lastName.value.trim();

    this.representativeForm.patchValue({ firstName, middleName, lastName }, { emitEvent: false });
  }

  private clearNames(): void {
    this.representativeForm.patchValue(
      { firstName: '', middleName: '', lastName: '' },
      { emitEvent: false },
    );
  }

  private applyModeValidation(): void {
    const firstName = this.representativeForm.controls.firstName;
    const lastName = this.representativeForm.controls.lastName;

    if (this.isLinkedMode()) {
      firstName.removeValidators([Validators.required, notBlankValidator()]);
      lastName.removeValidators([Validators.required, notBlankValidator()]);
    } else {
      firstName.addValidators([Validators.required, notBlankValidator()]);
      lastName.addValidators([Validators.required, notBlankValidator()]);
    }

    firstName.updateValueAndValidity({ emitEvent: false });
    lastName.updateValueAndValidity({ emitEvent: false });
  }

  private hasValidRepresentativeSource(): boolean {
    if (this.isLinkedMode()) {
      return this.selectedCustomer() !== null;
    }

    return true;
  }

  private createRequest(): RepresentativeAdditionDto {
    const formValue = this.representativeForm.getRawValue();
    const request: RepresentativeAdditionDto = {
      role: formValue.role,
      validFrom: formValue.validFrom,
    };

    if (formValue.mode === 'LINKED') {
      const selectedCustomer = this.selectedCustomer();
      if (selectedCustomer !== null) {
        request.individualCustomerId = selectedCustomer.id;
      }
      return request;
    }

    request.firstName = formValue.firstName;
    request.lastName = formValue.lastName;

    if (formValue.middleName.length > 0) {
      request.middleName = formValue.middleName;
    }

    return request;
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'The requested Legal Entity Customer could not be found.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this Customer.';
      }
    }

    return 'The Legal Entity Customer could not be loaded. Please try again.';
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
        return 'The Legal Entity or linked Individual Customer could not be found.';
      }
      if (error.status === 409) {
        return 'The Representative could not be added because the Customer state changed.';
      }
    }

    return 'The Representative could not be added. Please try again.';
  }
}
