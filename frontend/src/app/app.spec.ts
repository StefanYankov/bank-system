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

  const loginMock = vi.fn<
    () => Promise<void>
  >();

  const logoutMock = vi.fn<
    () => Promise<void>
  >();

  const hasRealmRoleMock = vi.fn<
    (role: string) => boolean
  >(
    (role: string): boolean =>
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

    loginMock.mockResolvedValue(undefined);
    logoutMock.mockResolvedValue(undefined);

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
      element.querySelector(
        '.app-header__brand',
      )?.textContent?.trim(),
    ).toBe('Bank System');
  });

  it('should provide the router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(
      element.querySelector('router-outlet'),
    ).not.toBeNull();
  });

  it('should render the application footer', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const footer = element.querySelector('.app-footer');

    expect(footer?.textContent).toContain('Bank System');

    expect(footer?.textContent).toContain(
      'NBU CSCB634 capstone project',
    );
  });

  it('should show login and no navigation for an anonymous user', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    );

    expect(button?.textContent).toContain('Log in');

    expect(
      element.querySelector('.user-summary'),
    ).toBeNull();

    expect(
      element.querySelector('.app-navigation'),
    ).toBeNull();

    expect(
      hasRealmRoleMock,
    ).not.toHaveBeenCalled();
  });

  it('should show anonymous landing guidance', () => {
    const fixture = TestBed.createComponent(App);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Bank operations in one place',
    );

    expect(element.textContent).toContain(
      'Log in with your Bank System account',
    );

    expect(element.textContent).not.toContain(
      'Employee management will be added in the next milestone',
    );

    expect(element.textContent).not.toContain(
      'Open protected page',
    );

    expect(element.textContent).not.toContain(
      'Open ADMIN page',
    );
  });

  it('should show My profile for an authenticated non-ADMIN user', () => {
    authenticated.set(true);
    username.set('loan.officer');
    realmRoles.set(['LOAN_OFFICER']);

    const fixture = TestBed.createComponent(App);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const navigation = element.querySelector(
      '.app-navigation',
    );

    expect(navigation?.textContent).toContain(
      'My profile',
    );

    expect(navigation?.textContent).not.toContain(
      'Employee administration',
    );

    expect(
      element.querySelector(
        'a[href="/current-employee"]',
      ),
    ).not.toBeNull();

    expect(
      element.querySelector(
        'a[href="/admin/employees"]',
      ),
    ).toBeNull();

    expect(
      element.querySelector('.role-badge'),
    ).toBeNull();

    expect(
      hasRealmRoleMock,
    ).toHaveBeenCalledWith('ADMIN');
  });

  it('should show Employee administration and My profile for an ADMIN user', () => {
    authenticated.set(true);
    username.set('bank.admin');
    realmRoles.set(['ADMIN']);

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const navigation = element.querySelector(
      '.app-navigation',
    );

    expect(navigation?.textContent).toContain(
      'Employee administration',
    );

    expect(navigation?.textContent).toContain(
      'My profile',
    );

    expect(
      element.querySelector(
        'a[href="/admin/employees"]',
      ),
    ).not.toBeNull();

    expect(
      element.querySelector(
        'a[href="/current-employee"]',
      ),
    ).not.toBeNull();

    expect(
      element.querySelector(
        '.user-summary__name',
      )?.textContent,
    ).toContain('bank.admin');

    expect(
      element.querySelector(
        '.role-badge',
      )?.textContent,
    ).toContain('ADMIN');

    expect(
      hasRealmRoleMock,
    ).toHaveBeenCalledWith('ADMIN');
  });

  it('should show current landing guidance for an authenticated user', () => {
    authenticated.set(true);
    username.set('loan.officer');
    realmRoles.set(['LOAN_OFFICER']);

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(
      'Use the navigation above to open your Employee profile',
    );
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
    realmRoles.set(['ADMIN']);

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
