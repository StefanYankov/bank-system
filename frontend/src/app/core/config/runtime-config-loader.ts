import {RuntimeConfig} from './runtime-config';

/**
 * Loads and validates the public runtime configuration.
 *
 * @returns the validated application configuration
 * @throws Error when the file cannot be loaded or contains invalid values
 */
export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const configUrl = new URL('config.json', document.baseURI);

  const response = await fetch(configUrl, {
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error(
      `Could not load runtime configuration: HTTP ${response.status}`,
    );
  }

  const data: unknown = await response.json();

  return parseRuntimeConfig(data);
}

function parseRuntimeConfig(value: unknown): RuntimeConfig {
  const root = requireObject(value, 'configuration');
  const keycloak = requireObject(root['keycloak'], 'keycloak');

  return {
    keycloak: {
      url: requireString(keycloak['url'], 'keycloak.url'),
      realm: requireString(keycloak['realm'], 'keycloak.realm'),
      clientId: requireString(keycloak['clientId'], 'keycloak.clientId'),
    },
    employeeApiUrl: requireString(
      root['employeeApiUrl'],
      'employeeApiUrl',
    ),
  };
}

function requireObject(
  value: unknown,
  fieldName: string,
): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(
      `Runtime configuration field '${fieldName}' must be an object`,
    );
  }

  return value as Record<string, unknown>;
}

function requireString(value: unknown, fieldName: string): string {
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw new Error(
      `Runtime configuration field '${fieldName}' must be a non-empty string`,
    );
  }

  return value.trim();
}
