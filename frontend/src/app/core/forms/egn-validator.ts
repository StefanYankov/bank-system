import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

const EGN_PATTERN = /^[0-9]{10}$/;
const CHECK_DIGIT_WEIGHTS = [2, 4, 8, 5, 10, 9, 7, 3, 6] as const;

/**
 * Validates the structural rules of a Bulgarian EGN.
 *
 * Blank values are delegated to required and not-blank validators. A non-blank
 * value must contain ten digits, encode a valid birthdate, and contain the
 * expected check digit. Customer Service repeats these checks and remains the
 * authoritative validation boundary.
 *
 * @returns Angular validator for Bulgarian personal identifiers
 */
export function egnValidator(): ValidatorFn {
  return (
    control: AbstractControl<string>,
  ): ValidationErrors | null => {
    const egn = control.value.trim();

    if (egn.length === 0) {
      return null;
    }

    if (!EGN_PATTERN.test(egn)) {
      return {egn: true};
    }

    if (!hasValidBirthDate(egn) || !hasValidCheckDigit(egn)) {
      return {egn: true};
    }

    return null;
  };
}

function hasValidBirthDate(egn: string): boolean {
  const yearOfCentury = twoDigitsAt(egn, 0);
  const encodedMonth = twoDigitsAt(egn, 2);
  const day = twoDigitsAt(egn, 4);

  let century: number;
  let month: number;

  if (encodedMonth >= 1 && encodedMonth <= 12) {
    century = 1900;
    month = encodedMonth;
  } else if (encodedMonth >= 21 && encodedMonth <= 32) {
    century = 1800;
    month = encodedMonth - 20;
  } else if (encodedMonth >= 41 && encodedMonth <= 52) {
    century = 2000;
    month = encodedMonth - 40;
  } else {
    return false;
  }

  const year = century + yearOfCentury;
  const date = new Date(Date.UTC(year, month - 1, day));

  return date.getUTCFullYear() === year
    && date.getUTCMonth() === month - 1
    && date.getUTCDate() === day;
}

function hasValidCheckDigit(egn: string): boolean {
  const sum = CHECK_DIGIT_WEIGHTS.reduce(
    (total, weight, index) => total + digitAt(egn, index) * weight,
    0,
  );
  const remainder = sum % 11;
  const expectedCheckDigit = remainder === 10 ? 0 : remainder;

  return digitAt(egn, 9) === expectedCheckDigit;
}

function twoDigitsAt(value: string, index: number): number {
  return digitAt(value, index) * 10 + digitAt(value, index + 1);
}

function digitAt(value: string, index: number): number {
  return Number(value[index]);
}
