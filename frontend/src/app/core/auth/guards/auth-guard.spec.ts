import {ActivatedRouteSnapshot, RouterStateSnapshot,} from '@angular/router';
import {AuthGuardData} from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {isAuthenticated} from './auth-guard';

describe('isAuthenticated', () => {
  const loginMock = vi.fn();

  const route = {} as ActivatedRouteSnapshot;
  const state = {
    url: '/protected',
  } as RouterStateSnapshot;

  beforeEach(() => {
    vi.clearAllMocks();
    loginMock.mockResolvedValue(undefined);
  });

  it('should allow an authenticated user', async () => {
    // Arrange
    const authData = createAuthData(true);

    // Act
    const result = await isAuthenticated(
      route,
      state,
      authData,
    );

    // Assert
    expect(result).toBe(true);
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('should redirect an anonymous user to Keycloak login', async () => {
    // Arrange
    const authData = createAuthData(false);

    // Act
    const result = await isAuthenticated(
      route,
      state,
      authData,
    );

    // Assert
    expect(result).toBe(false);
    expect(loginMock).toHaveBeenCalledOnce();
    expect(loginMock).toHaveBeenCalledWith({
      redirectUri: new URL(
        '/protected',
        window.location.origin,
      ).toString(),
    });
  });

  function createAuthData(
    authenticated: boolean,
  ): AuthGuardData {
    return {
      authenticated,
      grantedRoles: {
        realmRoles: [],
        resourceRoles: {},
      },
      keycloak: {
        login: loginMock,
      } as unknown as Keycloak,
    };
  }
});
