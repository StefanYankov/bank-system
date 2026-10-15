import {inject} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree,} from '@angular/router';
import {AuthGuardData, createAuthGuard,} from 'keycloak-angular';

/**
 * Route-data key containing the accepted Keycloak realm roles.
 */
export const ROUTE_ROLES = 'roles';

/**
 * Requires authentication and at least one realm role declared by the route.
 */
export async function hasRequiredRole(
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authData: AuthGuardData,
): Promise<boolean | UrlTree> {
  if (!authData.authenticated) {
    const redirectUri = new URL(
      state.url,
      window.location.origin,
    ).toString();

    await authData.keycloak.login({
      redirectUri,
    });

    return false;
  }

  const requiredRoles =
    route.data[ROUTE_ROLES] as readonly string[] | undefined;

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const accessGranted = requiredRoles.some((role) =>
    authData.grantedRoles.realmRoles.includes(role),
  );

  if (accessGranted) {
    return true;
  }

  return inject(Router).createUrlTree(['/forbidden']);
}

export const roleGuard: CanActivateFn =
  createAuthGuard<CanActivateFn>(hasRequiredRole);
