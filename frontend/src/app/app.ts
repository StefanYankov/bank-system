import {Component, inject, signal} from '@angular/core';
import {RouterOutlet} from '@angular/router';

import {AuthSession} from './core/auth/auth-session';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('Bank System');

  protected readonly authSession = inject(AuthSession);

  protected async login(): Promise<void> {
    await this.authSession.login();
  }

  protected async logout(): Promise<void> {
    await this.authSession.logout();
  }
}
