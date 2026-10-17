import {AbstractControl, ValidationErrors, ValidatorFn,} from '@angular/forms';

/**
 * Rejects string values that are empty or contain only whitespace.
 *
 * Angular's required validator rejects an empty string, but the backend's
 * Jakarta Validation @NotBlank constraint also rejects whitespace-only input.
 *
 * @returns Angular validator for non-blank string controls
 */
export function notBlankValidator(): ValidatorFn {
  return (
    control: AbstractControl<string>,
  ): ValidationErrors | null => {
    const value = control.value;

    if (value.trim().length === 0) {
      return {
        notBlank: true
      };
    } else {
      return null;
    }
  };
}
