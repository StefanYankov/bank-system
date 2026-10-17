import assert from 'node:assert/strict';
import {describe, it,} from 'node:test';
import {createTypeScriptSource, extractConstraints, getSchemas,} from './generate-form-constraints.mjs';

const EXPECTED_TEST_SCHEMAS = {
  EmployeeRegistrationDto: [
    'firstName',
    'workEmail',
    'position',
  ],
};

/**
 * Creates a minimal OpenAPI document for constraint-extractor tests.
 *
 * @returns {Record<string, unknown>} OpenAPI test contract
 */
function createOpenApiDocument() {
  return {
    openapi: '3.1.0',
    components: {
      schemas: {
        EmployeeRegistrationDto: {
          required: [
            'firstName',
            'workEmail',
            'position',
          ],
          properties: {
            firstName: {
              type: 'string',
              minLength: 0,
              maxLength: 100,
            },
            workEmail: {
              type: 'string',
              minLength: 0,
              maxLength: 254,
              format: 'email',
            },
            position: {
              type: 'string',
            },
          },
        },
      },
    },
  };
}

describe('Employee form constraint extractor', () => {
  it('should extract required, length, and format constraints', () => {
    const schemas = getSchemas(
      createOpenApiDocument(),
    );

    const constraints = extractConstraints(
      schemas,
      EXPECTED_TEST_SCHEMAS,
    );

    assert.deepStrictEqual(
      constraints,
      {
        EmployeeRegistrationDto: {
          firstName: {
            required: true,
            maxLength: 100,
            minLength: 0,
          },
          workEmail: {
            required: true,
            maxLength: 254,
            minLength: 0,
            format: 'email',
          },
          position: {
            required: true,
          },
        },
      },
    );
  });

  it('should preserve a zero minimum length', () => {
    const schemas = getSchemas(
      createOpenApiDocument(),
    );

    const constraints = extractConstraints(
      schemas,
      EXPECTED_TEST_SCHEMAS,
    );

    assert.strictEqual(
      constraints
        .EmployeeRegistrationDto
        .firstName
        .minLength,
      0,
    );
  });

  it('should fail when an expected schema is missing', () => {
    const schemas = getSchemas({
      components: {
        schemas: {},
      },
    });

    assert.throws(
      () => extractConstraints(
        schemas,
        {
          EmployeeRegistrationDto: [
            'firstName',
          ],
        },
      ),
      {
        message:
          'Expected OpenAPI schema '
          + '"EmployeeRegistrationDto" was not found.',
      },
    );
  });

  it('should fail when an expected property is missing', () => {
    const schemas = getSchemas(
      createOpenApiDocument(),
    );

    assert.throws(
      () => extractConstraints(
        schemas,
        {
          EmployeeRegistrationDto: [
            'unknownProperty',
          ],
        },
      ),
      {
        message:
          'Expected property '
          + '"EmployeeRegistrationDto.unknownProperty" '
          + 'was not found.',
      },
    );
  });

  it('should generate deterministic TypeScript source', () => {
    const constraints = {
      EmployeeRegistrationDto: {
        firstName: {
          required: true,
          maxLength: 100,
          minLength: 0,
        },
      },
    };

    const firstOutput = createTypeScriptSource(
      constraints,
    );

    const secondOutput = createTypeScriptSource(
      constraints,
    );

    assert.strictEqual(
      firstOutput,
      secondOutput,
    );

    assert.match(
      firstOutput,
      /Auto-generated from openapi\/employee-service\.json/,
    );

    assert.match(
      firstOutput,
      /"maxLength": 100/,
    );

    assert.match(
      firstOutput,
      /"minLength": 0/,
    );

    assert.match(
      firstOutput,
      /as const;/,
    );
  });
});
