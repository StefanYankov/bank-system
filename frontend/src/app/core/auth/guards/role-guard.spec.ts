import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree,} from '@angular/router';
import {AuthGuardData} from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {hasRequiredRole, ROUTE_ROLES,} from './role-guard';

describe('hasRequiredRole', () => {
  const loginMock = vi.fn();

  const state = {
    url: '/admin',
  } as RouterStateSnapshot;

  beforeEach(() => {
    vi.clearAllMocks();
    loginMock.mockResolvedValue(undefined);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
      ],
    });
  });

  it('should redirect an anonymous user to Keycloak login', async () => {
    // Arrange
    const route = createRoute(['ADMIN']);
    const authData = createAuthData(false, []);

    // Act
    const result = await TestBed.runInInjectionContext(() =>
      hasRequiredRole(route, state, authData),
    );

    // Assert
    expect(result).toBe(false);
    expect(loginMock).toHaveBeenCalledOnce();
    expect(loginMock).toHaveBeenCalledWith({
      redirectUri: new URL(
        '/admin',
        window.location.origin,
      ).toString(),
    });
  });

  it('should allow an authenticated route with no required roles', async () => {
    // Arrange
    const route = createRoute();
    const authData = createAuthData(true, ['CLERK']);

    // Act
    const result = await TestBed.runInInjectionContext(() =>
      hasRequiredRole(route, state, authData),
    );

    // Assert
    expect(result).toBe(true);
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('should allow a user with one of the required roles', async () => {
    // Arrange
    const route = createRoute([
      'ADMIN',
      'LOAN_OFFICER',
    ]);
    const authData = createAuthData(
      true,
      ['LOAN_OFFICER'],
    );

    // Act
    const result = await TestBed.runInInjectionContext(() =>
      hasRequiredRole(route, state, authData),
    );

    // Assert
    expect(result).toBe(true);
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('should redirect a user without a required role', async () => {
    // Arrange
    const route = createRoute(['ADMIN']);
    const authData = createAuthData(true, ['CLERK']);

    // Act
    const result = await TestBed.runInInjectionContext(() =>
      hasRequiredRole(route, state, authData),
    );

    // Assert
    expect(result).toBeInstanceOf(UrlTree);

    const router = TestBed.inject(Router);

    expect(
      router.serializeUrl(result as UrlTree),
    ).toBe('/forbidden');

    expect(loginMock).not.toHaveBeenCalled();
  });

  function createRoute(
    roles?: readonly string[],
  ): ActivatedRouteSnapshot {
    return {
      data: roles
        ? {
          [ROUTE_ROLES]: roles,
        }
        : {},
    } as unknown as ActivatedRouteSnapshot;
  }

  function createAuthData(
    authenticated: boolean,
    realmRoles: string[],
  ): AuthGuardData {
    return {
      authenticated,
      grantedRoles: {
        realmRoles,
        resourceRoles: {},
      },
      keycloak: {
        login: loginMock,
      } as unknown as Keycloak,
    };
  }
});
