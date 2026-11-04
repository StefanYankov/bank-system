import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import { egnValidator } from './egn-validator';

describe('egnValidator', () => {
  const validator = egnValidator();

  it.each(['6101047500', '0041010002', '9921010007', '0042290000', '0041019000'])(
    'should accept the backend valid vector %s',
    (egn) => {
      // Arrange
      const control = new FormControl(egn, { nonNullable: true });

      // Act
      const result = validator(control);

      // Assert
      expect(result).toBeNull();
    },
  );

  it.each([
    '610104750',
    '61010475000',
    '61010475A0',
    '6101 47500',
    '6100047500',
    '6113047500',
    '6120047500',
    '6101007500',
    '6102297500',
    '6101047501',
    '9999999999',
  ])('should reject the backend invalid vector %s', (egn) => {
    // Arrange
    const control = new FormControl(egn, { nonNullable: true });

    // Act
    const result = validator(control);

    // Assert
    expect(result).toEqual({ egn: true });
  });

  it.each(['', '   '])('should leave blank value %j to required validation', (egn) => {
    // Arrange
    const control = new FormControl(egn, { nonNullable: true });

    // Act & Assert
    expect(validator(control)).toBeNull();
  });
});
