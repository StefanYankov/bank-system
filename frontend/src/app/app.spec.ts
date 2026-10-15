import {signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {beforeEach, describe, expect, it, vi,} from 'vitest';

import {App} from './app';
import {AuthSession} from './core/auth/auth-session';

describe('App', () => {
  const authenticated = signal(false);
  const username = signal<string | null>(null);
  const realmRoles = signal<readonly string[]>([]);

  const loginMock = vi.fn<() => Promise<void>>().mockResolvedValue(undefined);
  const logoutMock = vi.fn<() => Promise<void>>().mockResolvedValue(undefined);
  const hasRealmRoleMock = vi.fn<(role: string) => boolean>((role: string): boolean =>
    realmRoles().includes(role),
  );

  const authSessionMock = {
    authenticated: authenticated.asReadonly(),
    username: username.asReadonly(),
    realmRoles: realmRoles.asReadonly(),
    hasRealmRole: hasRealmRoleMock,
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
        provideRouter([]),
        {
          provide: AuthSession,
          useValue: authSessionMock,
        },
      ],
    }).compileComponents();
  });

  it('should create the application', () => {
    const fixture = TestBed.createComponent(App);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the application title', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('.app-header__brand')?.textContent?.trim(),
    ).toBe('Bank System');
  });

  it('should provide the router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('router-outlet')).not.toBeNull();
  });

  it('should show login for an anonymous user', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector('.session-button');

    expect(button?.textContent).toContain('Log in');
    expect(element.querySelector('.user-summary')).toBeNull();
    expect(hasRealmRoleMock).not.toHaveBeenCalled();
  });

  it('should show user details and the ADMIN badge when the user has the ADMIN role', () => {
    authenticated.set(true);
    username.set('bank.admin');
    realmRoles.set(['ADMIN']);

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('.user-summary__name')?.textContent,
    ).toContain('bank.admin');

    expect(
      element.querySelector('.role-badge')?.textContent,
    ).toContain('ADMIN');

    expect(hasRealmRoleMock).toHaveBeenCalledWith('ADMIN');
  });

  it('should not show the ADMIN badge when the authenticated user lacks the ADMIN role', () => {
    authenticated.set(true);
    username.set('bank.teller');
    realmRoles.set(['TELLER']);

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(
      element.querySelector('.user-summary__name')?.textContent,
    ).toContain('bank.teller');

    expect(element.querySelector('.role-badge')).toBeNull();
    expect(hasRealmRoleMock).toHaveBeenCalledWith('ADMIN');
  });

  it('should delegate login to the authentication session', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    ) as HTMLButtonElement | null;

    button?.click();

    expect(loginMock).toHaveBeenCalledOnce();
    expect(logoutMock).not.toHaveBeenCalled();
  });

  it('should delegate logout to the authentication session', () => {
    authenticated.set(true);
    username.set('bank.admin');

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    ) as HTMLButtonElement | null;

    button?.click();

    expect(logoutMock).toHaveBeenCalledOnce();
    expect(loginMock).not.toHaveBeenCalled();
  });
});
