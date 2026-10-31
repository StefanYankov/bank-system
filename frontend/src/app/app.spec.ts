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
    // Arrange & Act
    const fixture = TestBed.createComponent(App);

    // Assert
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the application title', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(
      element.querySelector(
        '.app-header__brand',
      )?.textContent?.trim(),
    ).toBe('Bank System');
  });

  it('should provide the router outlet', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(
      element.querySelector('router-outlet'),
    ).not.toBeNull();
  });

  it('should render the application footer', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const footer = element.querySelector('.app-footer');

    // Assert
    expect(footer?.textContent).toContain('Bank System');
    expect(footer?.textContent).toContain(
      'NBU CSCB634 capstone project',
    );
  });

  it('should show login and no navigation for an anonymous user', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    );

    // Assert
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
    // Arrange
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain(
      'Bank operations in one place',
    );
    expect(element.textContent).toContain(
      'Log in with your Bank System account',
    );
  });

  it.each([
    ['CLERK'],
    ['LOAN_OFFICER'],
    ['ADMIN'],
  ])('should show Customers for the %s role', (role) => {
    // Arrange
    authenticated.set(true);
    username.set(role.toLowerCase());
    realmRoles.set([role]);
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(
      element.querySelector('a[href="/customers"]'),
    ).not.toBeNull();
  });

  it('should show My profile but not Employee administration for a non-ADMIN user', () => {
    // Arrange
    authenticated.set(true);
    username.set('loan.officer');
    realmRoles.set(['LOAN_OFFICER']);
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const navigation = element.querySelector(
      '.app-navigation',
    );

    // Assert
    expect(navigation?.textContent).toContain('Customers');
    expect(navigation?.textContent).toContain('My profile');
    expect(navigation?.textContent).not.toContain(
      'Employee administration',
    );
    expect(
      element.querySelector('a[href="/current-employee"]'),
    ).not.toBeNull();
    expect(
      element.querySelector('a[href="/admin/employees"]'),
    ).toBeNull();
    expect(
      element.querySelector('.role-badge'),
    ).toBeNull();
  });

  it('should show Employee administration, Customers, and My profile for an ADMIN user', () => {
    // Arrange
    authenticated.set(true);
    username.set('bank.admin');
    realmRoles.set(['ADMIN']);
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const navigation = element.querySelector(
      '.app-navigation',
    );

    // Assert
    expect(navigation?.textContent).toContain('Customers');
    expect(navigation?.textContent).toContain(
      'Employee administration',
    );
    expect(navigation?.textContent).toContain('My profile');
    expect(
      element.querySelector('a[href="/customers"]'),
    ).not.toBeNull();
    expect(
      element.querySelector('a[href="/admin/employees"]'),
    ).not.toBeNull();
    expect(
      element.querySelector('a[href="/current-employee"]'),
    ).not.toBeNull();
    expect(
      element.querySelector('.user-summary__name')?.textContent,
    ).toContain('bank.admin');
    expect(
      element.querySelector('.role-badge')?.textContent,
    ).toContain('ADMIN');
  });

  it('should show Customer landing guidance for an authenticated user', () => {
    // Arrange
    authenticated.set(true);
    username.set('loan.officer');
    realmRoles.set(['LOAN_OFFICER']);
    const fixture = TestBed.createComponent(App);

    // Act
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Assert
    expect(element.textContent).toContain(
      'Use the navigation above to search Customers',
    );
  });

  it('should delegate login to the authentication session', () => {
    // Arrange
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      '.session-button',
    ) as HTMLButtonElement | null;

    // Act
    button?.click();

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
    ) as HTMLButtonElement | null;

    // Act
    button?.click();

    // Assert
    expect(logoutMock).toHaveBeenCalledOnce();
    expect(loginMock).not.toHaveBeenCalled();
  });
});
