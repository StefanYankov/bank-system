import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  LegalEntityCustomerAPIService,
  LegalEntityCustomerViewDto,
  RepresentationEndDto,
  RepresentativeViewDto,
} from '../../../../../generated/customer-api';

/**
 * Confirms and ends a current Legal Entity Customer representation.
 *
 * The final valid date is inclusive and cannot precede the first valid date.
 * Ending preserves the Representative record and Customer link for history.
 */
@Component({
  selector: 'app-legal-entity-representation-end',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './legal-entity-representation-end.html',
  styleUrl: './legal-entity-representation-end.css',
})
export class LegalEntityRepresentationEnd implements OnInit {
  protected readonly customer = signal<LegalEntityCustomerViewDto | null>(null);
  protected readonly representative = signal<RepresentativeViewDto | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly dateError = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly submissionError = signal<string | null>(null);

  private readonly formBuilder = inject(NonNullableFormBuilder);
  protected readonly endForm = this.formBuilder.group({
    validTo: this.formBuilder.control('', [Validators.required]),
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
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loadError.set(this.loadErrorMessage(error));
          this.loading.set(false);
        },
      });
  }

  /**
   * Builds the displayed full name of the Representative being ended.
   *
   * @returns recorded Representative name
   */
  protected representativeName(): string {
    const representative = this.representative();
    if (representative === null) {
      return '';
    }

    const parts = [representative.firstName, representative.middleName, representative.lastName];
    return parts.filter((part): part is string => part !== null && part.length > 0).join(' ');
  }

  /**
   * Ends the representation after validating the inclusive final date.
   */
  protected confirm(): void {
    const customer = this.customer();
    const representative = this.representative();

    if (customer === null || representative === null || this.submitting()) {
      return;
    }

    this.dateError.set(null);

    if (this.endForm.invalid) {
      this.endForm.markAllAsTouched();
      return;
    }

    const validTo = this.endForm.controls.validTo.value;
    if (validTo < representative.validFrom) {
      this.dateError.set('Valid-to date cannot be before the valid-from date.');
      return;
    }

    const representationEndDto: RepresentationEndDto = { validTo };
    this.submitting.set(true);
    this.submissionError.set(null);

    this.customerApi
      .endRepresentation({
        id: customer.id,
        representativeId: representative.id,
        representationEndDto,
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
        return 'The final valid date is invalid. Review the date and try again.';
      }
      if (error.status === 403) {
        return 'You do not have permission to change this Customer.';
      }
      if (error.status === 404) {
        return 'The Legal Entity Customer or Representative could not be found.';
      }
      if (error.status === 409) {
        return 'The representation is already ended or the Customer state changed.';
      }
    }

    return 'The representation could not be ended. Please try again.';
  }
}
