import {signal, WritableSignal,} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {KEYCLOAK_EVENT_SIGNAL, KeycloakEvent, KeycloakEventType,} from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import {afterEach, beforeEach, describe, expect, it, vi,} from 'vitest';

import {AuthSession} from './auth-session';

describe('AuthSession', () => {
  let keycloakMock: Keycloak;
  let keycloakEvent: WritableSignal<KeycloakEvent>;

  const loginMock = vi.fn();
  const logoutMock = vi.fn();

  beforeEach(() => {
    loginMock.mockResolvedValue(undefined);
    logoutMock.mockResolvedValue(undefined);

    keycloakMock = {
      authenticated: false,
      tokenParsed: undefined,
      realmAccess: undefined,
      login: loginMock,
      logout: logoutMock,
    } as unknown as Keycloak;

    keycloakEvent = signal<KeycloakEvent>({
      type: KeycloakEventType.Ready,
      args: false,
    });

    TestBed.configureTestingModule({
      providers: [
        AuthSession,
        {
          provide: Keycloak,
          useValue: keycloakMock,
        },
        {
          provide: KEYCLOAK_EVENT_SIGNAL,
          useValue: keycloakEvent.asReadonly(),
        },
      ],
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should expose empty state when the user is not authenticated', () => {
    // Act
    const session = TestBed.inject(AuthSession);

    TestBed.tick();

    // Assert
    expect(session.authenticated()).toBe(false);
    expect(session.username()).toBeNull();
    expect(session.realmRoles()).toEqual([]);
  });

  it('should expose the authenticated username and realm roles', () => {
    // Arrange
    keycloakMock.authenticated = true;
    keycloakMock.tokenParsed = {
      preferred_username: 'bank.admin',
    };
    keycloakMock.realmAccess = {
      roles: ['ADMIN', 'offline_access'],
    };

    // Act
    const session = TestBed.inject(AuthSession);
    TestBed.tick();

    // Assert
    expect(session.authenticated()).toBe(true);
    expect(session.username()).toBe('bank.admin');
    expect(session.realmRoles()).toEqual([
      'ADMIN',
      'offline_access',
    ]);
    expect(session.hasRealmRole('ADMIN')).toBe(true);
    expect(session.hasRealmRole('CLERK')).toBe(false);
  });

  it('should clear authentication state after a logout event', () => {
    // Arrange
    keycloakMock.authenticated = true;
    keycloakMock.tokenParsed = {
      preferred_username: 'bank.admin',
    };
    keycloakMock.realmAccess = {
      roles: ['ADMIN'],
    };

    const session = TestBed.inject(AuthSession);
    TestBed.tick();

    expect(session.authenticated()).toBe(true);

    // Act
    keycloakMock.authenticated = false;
    keycloakMock.tokenParsed = undefined;
    keycloakMock.realmAccess = undefined;

    keycloakEvent.set({
      type: KeycloakEventType.AuthLogout,
    });

    TestBed.tick();

    // Assert
    expect(session.authenticated()).toBe(false);
    expect(session.username()).toBeNull();
    expect(session.realmRoles()).toEqual([]);
  });

  it('should delegate login with the current browser URL', async () => {
    // Arrange
    const session = TestBed.inject(AuthSession);
    TestBed.tick();

    // Act
    await session.login();

    // Assert
    expect(loginMock).toHaveBeenCalledOnce();
    expect(loginMock).toHaveBeenCalledWith({
      redirectUri: window.location.href,
    });
  });

  it('should delegate logout with the application base URL', async () => {
    // Arrange
    const session = TestBed.inject(AuthSession);
    TestBed.tick();

    // Act
    await session.logout();

    // Assert
    expect(logoutMock).toHaveBeenCalledOnce();
    expect(logoutMock).toHaveBeenCalledWith({
      redirectUri: document.baseURI,
    });
  });
});
