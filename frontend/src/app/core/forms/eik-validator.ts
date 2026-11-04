import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const EIK_PATTERN = /^\d{9}$/;
const FIRST_PASS_WEIGHTS = [1, 2, 3, 4, 5, 6, 7, 8];
const SECOND_PASS_WEIGHTS = [3, 4, 5, 6, 7, 8, 9, 10];

/**
 * Creates a validator for a 9-digit Bulgarian EIK.
 *
 * The ninth digit is a check digit. The weighted sum of the first eight
 * digits is taken modulo 11 with weights 1 to 8. A remainder of 10 is
 * recalculated with weights 3 to 10, and a second remainder of 10 becomes 0.
 * Empty values are accepted so that required-field validation stays separate.
 * Customer Service remains authoritative.
 *
 * @returns a validator reporting {@code eik} for an invalid value
 */
export function eikValidator(): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === undefined || value.length === 0) {
      return null;
    }
    return isValidEik(value) ? null : { eik: true };
  };
}

function isValidEik(value: string): boolean {
  if (!EIK_PATTERN.test(value)) {
    return false;
  }
  const digits = Array.from(value, Number);
  let checkDigit = weightedRemainder(digits, FIRST_PASS_WEIGHTS);
  if (checkDigit === 10) {
    checkDigit = weightedRemainder(digits, SECOND_PASS_WEIGHTS);
    if (checkDigit === 10) {
      checkDigit = 0;
    }
  }
  return checkDigit === digits[8];
}

function weightedRemainder(digits: readonly number[], weights: readonly number[]): number {
  const sum = weights.reduce((total, weight, index) => total + weight * (digits[index] ?? 0), 0);
  return sum % 11;
}
