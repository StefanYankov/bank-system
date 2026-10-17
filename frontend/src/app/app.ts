import {Component, inject, signal,} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';

import {AuthSession} from './core/auth/auth-session';

/**
 * Provides the Bank System application shell.
 *
 * The shell owns the application header, role-aware navigation, session
 * controls, landing content, footer, and routed page area.
 */
@Component({
  selector: 'app-root',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {

  protected readonly title = signal('Bank System');

  protected readonly authSession = inject(AuthSession);

  /**
   * Starts the Keycloak login flow.
   */
  protected async login(): Promise<void> {
    await this.authSession.login();
  }

  /**
   * Ends the Keycloak session.
   */
  protected async logout(): Promise<void> {
    await this.authSession.logout();
  }
}
