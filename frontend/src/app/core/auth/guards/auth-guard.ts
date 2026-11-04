import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { AuthGuardData, createAuthGuard } from 'keycloak-angular';

/**
 * Requires Keycloak authentication before activating a route.
 */
export async function isAuthenticated(
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authData: AuthGuardData,
): Promise<boolean> {
  if (authData.authenticated) {
    return true;
  }

  const redirectUri = new URL(state.url, window.location.origin).toString();

  await authData.keycloak.login({
    redirectUri,
  });

  return false;
}

export const authGuard: CanActivateFn = createAuthGuard<CanActivateFn>(isAuthenticated);
