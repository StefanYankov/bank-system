import {FormControl} from '@angular/forms';
import {describe, expect, it} from 'vitest';
import {eikValidator} from './eik-validator';

describe('eikValidator', () => {
  const validate = (value: string) =>
    eikValidator()(new FormControl(value, {nonNullable: true}));

  it('should accept an EIK with a valid first-pass check digit', () => {
    // Act
    const result = validate('123456786');

    // Assert
    expect(result).toBeNull();
  });

  it('should accept an EIK that needs the second-pass weights', () => {
    // Act
    const result = validate('100000086');

    // Assert
    expect(result).toBeNull();
  });

  it('should reject an EIK with a wrong check digit', () => {
    // Act
    const result = validate('123456789');

    // Assert
    expect(result).toEqual({eik: true});
  });

  it.each([
    ['12345678'],
    ['1234567860'],
    ['12345678a'],
    ['1234 5678'],
  ])('should reject the malformed value %s', (value) => {
    // Act
    const result = validate(value);

    // Assert
    expect(result).toEqual({eik: true});
  });

  it('should leave an empty value to the required validator', () => {
    // Act
    const result = validate('');

    // Assert
    expect(result).toBeNull();
  });
});
