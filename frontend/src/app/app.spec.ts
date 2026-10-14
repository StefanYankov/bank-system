import {signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {App} from './app';
import {AuthSession} from './core/auth/auth-session';

describe('App', () => {
  const authenticated = signal(false);
  const username = signal<string | null>(null);
  const realmRoles = signal<readonly string[]>([]);

  const loginMock = vi.fn().mockResolvedValue(undefined);
  const logoutMock = vi.fn().mockResolvedValue(undefined);

  const authSessionMock = {
    authenticated: authenticated.asReadonly(),
    username: username.asReadonly(),
    realmRoles: realmRoles.asReadonly(),

    hasRealmRole: (role: string): boolean =>
      realmRoles().includes(role),

    login: loginMock,
    logout: logoutMock,
  };

  beforeEach(async () => {
    authenticated.set(false);
    username.set(null);
    realmRoles.set([]);

    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: AuthSession,
          useValue: authSessionMock,
        },
      ],
    }).compileComponents();
  });

  it('should create the application', () => {
    // Act
    const fixture = TestBed.createComponent(App);

    // Assert
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the application title', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('.app-header__brand')?.textContent,
    ).toContain('Bank System');
  });

  it('should provide the router outlet', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('router-outlet')).not.toBeNull();
  });

  it('should show login for an anonymous user', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    ) as HTMLButtonElement | null;

    expect(button?.textContent).toContain('Log in');
    expect(
      element.querySelector('.user-summary'),
    ).toBeNull();
  });

  it('should show the authenticated username, role, and logout action', () => {
    // Arrange
    authenticated.set(true);
    username.set('bank.admin');
    realmRoles.set(['ADMIN']);

    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('.user-summary__name')?.textContent,
    ).toContain('bank.admin');

    expect(
      element.querySelector('.role-badge')?.textContent,
    ).toContain('ADMIN');

    expect(
      element.querySelector('.session-button')?.textContent,
    ).toContain('Log out');
  });

  it('should delegate login to the authentication session', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    ) as HTMLButtonElement;

    // Act
    button.click();

    // Assert
    expect(loginMock).toHaveBeenCalledOnce();
    expect(logoutMock).not.toHaveBeenCalled();
  });

  it('should delegate logout to the authentication session', () => {
    // Arrange
    authenticated.set(true);
    username.set('bank.admin');
    realmRoles.set(['ADMIN']);

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    ) as HTMLButtonElement;

    // Act
    button.click();

    // Assert
    expect(logoutMock).toHaveBeenCalledOnce();
    expect(loginMock).not.toHaveBeenCalled();
  });
});
