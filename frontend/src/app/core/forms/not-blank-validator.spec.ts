import {FormControl} from '@angular/forms';
import {describe, expect, it,} from 'vitest';
import {notBlankValidator} from './not-blank-validator';

describe('notBlankValidator', () => {
  const validator = notBlankValidator();

  it('should accept text containing non-whitespace characters', () => {
    const control = new FormControl(
      '  Serafim  ',
      {
        nonNullable: true,
      },
    );

    expect(validator(control)).toBeNull();
  });

  it('should reject an empty value', () => {
    const control = new FormControl(
      '',
      {
        nonNullable: true,
      },
    );

    expect(validator(control)).toEqual({
      notBlank: true,
    });
  });

  it('should reject a whitespace-only value', () => {
    const control = new FormControl(
      '   ',
      {
        nonNullable: true,
      },
    );

    expect(validator(control)).toEqual({
      notBlank: true,
    });
  });
});
