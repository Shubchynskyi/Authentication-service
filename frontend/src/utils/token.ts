export type JwtPayload = { exp?: number };

let accessToken: string | null = null;
const AUTH_SESSION_KEY = 'auth.hasSession';

function canUseStorage(): boolean {
  return typeof window !== 'undefined' && !!window.localStorage;
}

/**
 * Validates JWT token format (must have 3 parts separated by dots)
 */
export function isValidJwtFormat(token: string | null | undefined): boolean {
  if (typeof token !== 'string' || token.length === 0) return false;
  const parts = token.split('.');
  return parts.length === 3 && parts.every(part => part.length > 0);
}

function decodePayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(payload);
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function isJwtExpired(token: string | null | undefined): boolean {
  if (!token) return true;
  const payload = decodePayload(token);
  if (!payload || typeof payload.exp !== 'number') return true;
  const now = Math.floor(Date.now() / 1000);
  return payload.exp <= now;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function setTokens(token: string): void {
  accessToken = token;
}

export function clearTokens(): void {
  accessToken = null;
}

export function hasAuthSession(): boolean {
  if (!canUseStorage()) return false;
  return window.localStorage.getItem(AUTH_SESSION_KEY) === 'true';
}

export function setAuthSessionActive(): void {
  if (!canUseStorage()) return;
  window.localStorage.setItem(AUTH_SESSION_KEY, 'true');
}

export function clearAuthSession(): void {
  if (!canUseStorage()) return;
  window.localStorage.removeItem(AUTH_SESSION_KEY);
}
