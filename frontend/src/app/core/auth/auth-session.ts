import { effect, inject, Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';

/**
 * Exposes authentication state and user actions to Angular components.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthSession {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  private readonly authenticatedState = signal(false);
  /**
   * Read-only authentication state exposed to components.
   */
  readonly authenticated = this.authenticatedState.asReadonly();
  private readonly usernameState = signal<string | null>(null);
  /**
   * Display name taken from the authenticated access token.
   */
  readonly username = this.usernameState.asReadonly();
  private readonly realmRolesState = signal<readonly string[]>([]);
  /**
   * Realm roles contained in the authenticated access token.
   */
  readonly realmRoles = this.realmRolesState.asReadonly();

  constructor() {
    effect(() => {
      this.keycloakEvent();

      this.synchronizeState();
    });
  }

  /**
   * Redirects the browser to Keycloak login.
   */
  async login(): Promise<void> {
    await this.keycloak.login({
      redirectUri: window.location.href,
    });
  }

  /**
   * Ends the Keycloak session and returns to the Angular application.
   */
  async logout(): Promise<void> {
    await this.keycloak.logout({
      redirectUri: document.baseURI,
    });
  }

  /**
   * Checks whether the authenticated user has a Keycloak realm role.
   */
  hasRealmRole(role: string): boolean {
    return this.realmRolesState().includes(role);
  }

  private synchronizeState(): void {
    const authenticated = this.keycloak.authenticated === true;

    this.authenticatedState.set(authenticated);

    if (!authenticated) {
      this.usernameState.set(null);
      this.realmRolesState.set([]);
      return;
    }

    const username = this.keycloak.tokenParsed?.['preferred_username'];

    this.usernameState.set(typeof username === 'string' ? username : null);

    this.realmRolesState.set(this.keycloak.realmAccess?.roles ?? []);
  }
}
