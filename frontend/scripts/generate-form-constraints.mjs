import {mkdirSync, readFileSync, writeFileSync,} from 'node:fs';
import {dirname} from 'node:path';
import {pathToFileURL} from 'node:url';

const OPENAPI_PATH = './openapi/employee-service.json';

const OUTPUT_PATH = './src/app/generated/schema-constraints.generated.ts';

/**
 * OpenAPI schemas and properties required by Employee frontend forms.
 *
 * Keeping the expected contract explicit makes generation fail when the
 * backend removes or renames a schema property used by the frontend.
 */
export const EXPECTED_SCHEMAS = {
  EmployeeRegistrationDto: [
    'identitySubjectId',
    'firstName',
    'lastName',
    'workEmail',
    'position',
    'employmentStartDate',
  ],
  EmployeeUpdateDto: [
    'firstName',
    'lastName',
    'workEmail',
  ],
};

/**
 * Reads and parses an OpenAPI JSON document.
 *
 * @param {string} filePath path to the OpenAPI document
 * @returns {Record<string, unknown>} parsed OpenAPI document
 */
export function readOpenApiDocument(filePath) {
  const fileContent = readFileSync(filePath)
    .toString('utf8');

  return JSON.parse(fileContent);
}

/**
 * Returns the component schemas from an OpenAPI document.
 *
 * @param {Record<string, unknown>} openApiDocument parsed OpenAPI document
 * @returns {Record<string, Record<string, unknown>>} OpenAPI schemas
 * @throws {Error} when components.schemas is missing or malformed
 */
export function getSchemas(openApiDocument) {
  const components = openApiDocument.components;

  if (!isRecord(components)) {
    throw new Error(
      'OpenAPI contract does not contain a valid components object.',
    );
  }

  const schemas = components.schemas;

  if (!isRecordOfRecords(schemas)) {
    throw new Error(
      'OpenAPI contract does not contain valid components.schemas.',
    );
  }

  return schemas;
}

/**
 * Extracts validation metadata from the expected Employee schemas.
 *
 * Constraint values are copied faithfully from OpenAPI. The extractor does
 * not decide whether a particular value should create an Angular validator.
 * For example, `minLength: 0` remains part of the generated metadata.
 *
 * @param {Record<string, Record<string, unknown>>} schemas OpenAPI schemas
 * @param {Record<string, readonly string[]>} expectedSchemas expected contract
 * @returns {Record<string, Record<string, Record<string, unknown>>>}
 * generated constraint metadata
 * @throws {Error} when an expected schema or property is missing
 */
export function extractConstraints(
  schemas,
  expectedSchemas = EXPECTED_SCHEMAS,
) {
  const constraints = {};

  for (
    const [schemaName, expectedProperties]
    of Object.entries(expectedSchemas)
    ) {
    const schema = schemas[schemaName];

    if (schema === undefined) {
      throw new Error(
        `Expected OpenAPI schema "${schemaName}" was not found.`,
      );
    }

    const properties = schema.properties;

    if (!isRecordOfRecords(properties)) {
      throw new Error(
        `OpenAPI schema "${schemaName}" has no valid properties.`,
      );
    }

    const requiredProperties = new Set(
      Array.isArray(schema.required)
        ? schema.required.filter(isString)
        : [],
    );

    const schemaConstraints = {};

    for (const propertyName of expectedProperties) {
      const property = properties[propertyName];

      if (property === undefined) {
        throw new Error(
          `Expected property "${schemaName}.${propertyName}" `
          + 'was not found.',
        );
      }

      schemaConstraints[propertyName] = {
        required: requiredProperties.has(propertyName),

        ...(isNumber(property.maxLength)
          ? {maxLength: property.maxLength}
          : {}),

        ...(isNumber(property.minLength)
          ? {minLength: property.minLength}
          : {}),

        ...(isString(property.pattern)
          ? {pattern: property.pattern}
          : {}),

        ...(isNumber(property.minimum)
          ? {minimum: property.minimum}
          : {}),

        ...(isNumber(property.maximum)
          ? {maximum: property.maximum}
          : {}),

        ...(isString(property.format)
          ? {format: property.format}
          : {}),
      };
    }

    constraints[schemaName] = schemaConstraints;
  }

  return constraints;
}

/**
 * Creates the generated TypeScript source.
 *
 * @param {Record<string, Record<string, Record<string, unknown>>>} constraints
 * extracted OpenAPI constraints
 * @returns {string} generated TypeScript source
 */
export function createTypeScriptSource(constraints) {
  return `/*
 * Auto-generated from openapi/employee-service.json.
 * Do not edit manually.
 */

export const employeeSchemaConstraints = ${JSON.stringify(
    constraints,
    null,
    2,
  )} as const;
`;
}

/**
 * Writes generated TypeScript constraints to the output path.
 *
 * @param {string} outputPath generated file path
 * @param {string} fileContent generated TypeScript source
 */
export function writeGeneratedFile(
  outputPath,
  fileContent,
) {
  mkdirSync(dirname(outputPath), {
    recursive: true,
  });

  writeFileSync(
    outputPath,
    fileContent,
    'utf8',
  );
}

/**
 * Determines whether a value is a non-array object.
 *
 * @param {unknown} value value to inspect
 * @returns {value is Record<string, unknown>}
 * true when the value is a record-like object
 */
export function isRecord(value) {
  return value !== null
    && typeof value === 'object'
    && !Array.isArray(value);
}

/**
 * Determines whether a value is a record whose values are also records.
 *
 * @param {unknown} value value to inspect
 * @returns {value is Record<string, Record<string, unknown>>}
 * true when the value contains only record-like values
 */
export function isRecordOfRecords(value) {
  return isRecord(value)
    && Object.values(value).every(isRecord);
}

/**
 * Determines whether a value is a string.
 *
 * @param {unknown} value value to inspect
 * @returns {value is string} true when the value is a string
 */
export function isString(value) {
  return typeof value === 'string';
}

/**
 * Determines whether a value is a finite number.
 *
 * Zero is intentionally accepted because an OpenAPI constraint such as
 * `minLength: 0` must be preserved faithfully in generated metadata.
 *
 * @param {unknown} value value to inspect
 * @returns {value is number} true when the value is a finite number
 */
export function isNumber(value) {
  return typeof value === 'number'
    && Number.isFinite(value);
}

/**
 * Generates Employee form constraints from an OpenAPI contract.
 *
 * @param {string} openApiPath input OpenAPI document
 * @param {string} outputPath generated TypeScript destination
 */
export function generateFormConstraints(
  openApiPath = OPENAPI_PATH,
  outputPath = OUTPUT_PATH,
) {
  const openApiDocument = readOpenApiDocument(
    openApiPath,
  );

  const schemas = getSchemas(openApiDocument);

  const constraints = extractConstraints(
    schemas,
  );

  const fileContent = createTypeScriptSource(
    constraints,
  );

  writeGeneratedFile(
    outputPath,
    fileContent,
  );

  console.log(
    `Generated Employee form constraints: ${outputPath}`,
  );
}

const executedFile = process.argv[1];

if (
  executedFile !== undefined
  && import.meta.url === pathToFileURL(executedFile).href
) {
  generateFormConstraints();
}
