import { describe, expect, it } from 'vitest';

import { createApiUrlPattern } from './api-url-pattern';

describe('createApiUrlPattern', () => {
  it('should match the configured API URL and paths below it', () => {
    // Arrange
    const pattern = createApiUrlPattern('http://localhost:8081');

    // Assert
    expect(pattern.test('http://localhost:8081')).toBe(true);

    expect(pattern.test('http://localhost:8081/api/employees')).toBe(true);
  });

  it('should match the Customer API URL and its paths', () => {
    // Arrange
    const pattern = createApiUrlPattern('http://localhost:8082');

    // Assert
    expect(pattern.test('http://localhost:8082/api/customers')).toBe(true);
    expect(pattern.test('http://localhost:8082/api/customers/individuals/123')).toBe(true);
    expect(pattern.test('http://localhost:8081/api/employees')).toBe(false);
  });

  it('should handle a configured URL with a trailing slash', () => {
    // Arrange
    const pattern = createApiUrlPattern('http://localhost:8081/');

    // Assert
    expect(pattern.test('http://localhost:8081/api/employees')).toBe(true);
  });

  it('should not match a similar port or unrelated URL', () => {
    // Arrange
    const pattern = createApiUrlPattern('http://localhost:8081');

    // Assert
    expect(pattern.test('http://localhost:80810/api/employees')).toBe(false);

    expect(pattern.test('http://localhost:8180/realms/bank-system')).toBe(false);

    expect(pattern.test('https://example.com/api/employees')).toBe(false);
  });
});
