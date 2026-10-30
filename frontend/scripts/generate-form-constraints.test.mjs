import assert from 'node:assert/strict';
import {describe, it,} from 'node:test';

import {
  createTypeScriptSource,
  extractConstraints,
  getSchemas,
  getServiceConfig,
  SERVICE_CONFIGS,
} from './generate-form-constraints.mjs';

const EXPECTED_TEST_SCHEMAS = {
  EmployeeRegistrationDto: ['firstName', 'workEmail', 'position',],
};

function createOpenApiDocument() {
  return {
    openapi: '3.1.0', components: {
      schemas: {
        EmployeeRegistrationDto: {
          required: ['firstName', 'workEmail', 'position',], properties: {
            firstName: {
              type: 'string', minLength: 0, maxLength: 100,
            }, workEmail: {
              type: 'string', minLength: 0, maxLength: 254, format: 'email',
            }, position: {
              type: 'string',
            },
          },
        },
      },
    },
  };
}

describe('Form constraint extractor', () => {
  it('should extract required, length, and format constraints', () => {
    const schemas = getSchemas(createOpenApiDocument(),);

    const constraints = extractConstraints(schemas, EXPECTED_TEST_SCHEMAS,);

    assert.deepStrictEqual(constraints, {
      EmployeeRegistrationDto: {
        firstName: {
          required: true, maxLength: 100, minLength: 0,
        }, workEmail: {
          required: true, maxLength: 254, minLength: 0, format: 'email',
        }, position: {
          required: true,
        },
      },
    },);
  });

  it('should preserve a zero minimum length', () => {
    const schemas = getSchemas(createOpenApiDocument(),);

    const constraints = extractConstraints(schemas, EXPECTED_TEST_SCHEMAS,);

    assert.strictEqual(constraints.EmployeeRegistrationDto.firstName.minLength, 0,);
  });

  it('should fail when an expected schema is missing', () => {
    const schemas = getSchemas({
      components: {
        schemas: {},
      },
    });

    assert.throws(() => extractConstraints(schemas, {
      EmployeeRegistrationDto: ['firstName',],
    },), {
      message: 'Expected OpenAPI schema ' + '"EmployeeRegistrationDto" was not found.',
    },);
  });

  it('should fail when an expected property is missing', () => {
    const schemas = getSchemas(createOpenApiDocument(),);

    assert.throws(() => extractConstraints(schemas, {
      EmployeeRegistrationDto: ['unknownProperty',],
    },), {
      message: 'Expected property ' + '"EmployeeRegistrationDto.unknownProperty" ' + 'was not found.',
    },);
  });

  it('should generate deterministic TypeScript source', () => {
    const constraints = {
      EmployeeRegistrationDto: {
        firstName: {
          required: true, maxLength: 100, minLength: 0,
        },
      },
    };

    const firstOutput = createTypeScriptSource(constraints, './openapi/employee-service.json', 'employeeSchemaConstraints',);

    const secondOutput = createTypeScriptSource(constraints, './openapi/employee-service.json', 'employeeSchemaConstraints',);

    assert.strictEqual(firstOutput, secondOutput,);

    assert.match(firstOutput, /Auto-generated from openapi\/employee-service\.json/,);

    assert.match(firstOutput, /"maxLength": 100/,);

    assert.match(firstOutput, /"minLength": 0/,);

    assert.match(firstOutput, /as const;/,);
  });

  it('should use the supplied source path and export name', () => {
    const output = createTypeScriptSource({}, './openapi/customer-service.json', 'customerSchemaConstraints',);

    assert.match(output, /Auto-generated from openapi\/customer-service\.json/,);

    assert.match(output, /export const customerSchemaConstraints = /,);
  });
});

describe('Service configuration', () => {
  it('should return the configuration of a known service', () => {
    const config = getServiceConfig('employee');

    assert.strictEqual(config.openApiPath, './openapi/employee-service.json',);

    assert.strictEqual(config.exportName, 'employeeSchemaConstraints',);
  });

  it('should fail for an unknown or missing service', () => {
    assert.throws(() => getServiceConfig('loan'), /Unknown service "loan"/,);

    assert.throws(() => getServiceConfig(undefined), /Unknown service "undefined"/,);
  });

  it('should give every service its own output file and export name', () => {
    const configs = Object.values(SERVICE_CONFIGS);

    assert.strictEqual(new Set(configs.map((config) => config.outputPath)).size, configs.length,);

    assert.strictEqual(new Set(configs.map((config) => config.exportName)).size, configs.length,);
  });
});
