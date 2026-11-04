import { afterEach, describe, expect, it, vi } from 'vitest';
import { loadRuntimeConfig } from './runtime-config-loader';

describe('loadRuntimeConfig', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should load, validate, and trim the runtime configuration', async () => {
    // Arrange
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          keycloak: {
            url: '  http://localhost:8180  ',
            realm: '  bank-system  ',
            clientId: '  bank-frontend  ',
          },
          employeeApiUrl: '  http://localhost:8081  ',
          customerApiUrl: '  http://localhost:8082  ',
        }),
        {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
        },
      ),
    );

    vi.stubGlobal('fetch', fetchMock);

    // Act
    const config = await loadRuntimeConfig();

    // Assert
    expect(config).toEqual({
      keycloak: {
        url: 'http://localhost:8180',
        realm: 'bank-system',
        clientId: 'bank-frontend',
      },
      employeeApiUrl: 'http://localhost:8081',
      customerApiUrl: 'http://localhost:8082',
    });

    expect(fetchMock).toHaveBeenCalledWith(new URL('config.json', document.baseURI), {
      cache: 'no-store',
    });
  });

  it('should reject an unsuccessful HTTP response', async () => {
    // Arrange
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(null, {
          status: 404,
        }),
      ),
    );

    // Act & Assert
    await expect(loadRuntimeConfig()).rejects.toThrow(
      'Could not load runtime configuration: HTTP 404',
    );
  });

  it('should reject a missing Keycloak configuration object', async () => {
    // Arrange
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            employeeApiUrl: 'http://localhost:8081',
            customerApiUrl: 'http://localhost:8082',
          }),
          {
            status: 200,
            headers: {
              'Content-Type': 'application/json',
            },
          },
        ),
      ),
    );

    // Act & Assert
    await expect(loadRuntimeConfig()).rejects.toThrow(
      "Runtime configuration field 'keycloak' must be an object",
    );
  });

  it('should reject a blank required configuration value', async () => {
    // Arrange
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            keycloak: {
              url: 'http://localhost:8180',
              realm: '   ',
              clientId: 'bank-frontend',
            },
            employeeApiUrl: 'http://localhost:8081',
            customerApiUrl: 'http://localhost:8082',
          }),
          {
            status: 200,
            headers: {
              'Content-Type': 'application/json',
            },
          },
        ),
      ),
    );

    // Act & Assert
    await expect(loadRuntimeConfig()).rejects.toThrow(
      "Runtime configuration field 'keycloak.realm' must be a non-empty string",
    );
  });

  it('should reject a missing Customer API URL', async () => {
    // Arrange
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            keycloak: {
              url: 'http://localhost:8180',
              realm: 'bank-system',
              clientId: 'bank-frontend',
            },
            employeeApiUrl: 'http://localhost:8081',
          }),
          {
            status: 200,
            headers: {
              'Content-Type': 'application/json',
            },
          },
        ),
      ),
    );

    // Act & Assert
    await expect(loadRuntimeConfig()).rejects.toThrow(
      "Runtime configuration field 'customerApiUrl' must be a non-empty string",
    );
  });
});
