export type JwtPayload = { exp?: number };

export type TokenStorageMode = 'local' | 'session';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const TOKEN_STORAGE_MODE_KEY = 'tokenStorageMode';

/**
 * Validates JWT token format (must have 3 parts separated by dots)
 */
export function isValidJwtFormat(token: string | null | undefined): boolean {
  if (!token || typeof token !== 'string') return false;
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

function safeGet(storage: Storage, key: string): string | null {
  try {
    return storage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(storage: Storage, key: string, value: string): void {
  try {
    storage.setItem(key, value);
  } catch {
    // ignore
  }
}

function safeRemove(storage: Storage, key: string): void {
  try {
    storage.removeItem(key);
  } catch {
    // ignore
  }
}

export function getTokenStorageMode(): TokenStorageMode {
  const raw = safeGet(localStorage, TOKEN_STORAGE_MODE_KEY);
  return raw === 'session' ? 'session' : 'local';
}

export function setTokenStorageMode(mode: TokenStorageMode): void {
  safeSet(localStorage, TOKEN_STORAGE_MODE_KEY, mode);
}

export function getAccessToken(): string | null {
  // Prefer current mode, but support reading from either storage for migration.
  const mode = getTokenStorageMode();
  const primary = mode === 'session' ? sessionStorage : localStorage;
  const secondary = mode === 'session' ? localStorage : sessionStorage;
  return safeGet(primary, ACCESS_TOKEN_KEY) ?? safeGet(secondary, ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  const mode = getTokenStorageMode();
  const primary = mode === 'session' ? sessionStorage : localStorage;
  const secondary = mode === 'session' ? localStorage : sessionStorage;
  return safeGet(primary, REFRESH_TOKEN_KEY) ?? safeGet(secondary, REFRESH_TOKEN_KEY);
}

export function setTokens(accessToken: string, refreshToken: string, mode: TokenStorageMode): void {
  const storage = mode === 'session' ? sessionStorage : localStorage;

  // Persist mode in localStorage so next reload knows where to read tokens from.
  setTokenStorageMode(mode);

  safeSet(storage, ACCESS_TOKEN_KEY, accessToken);
  safeSet(storage, REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens(): void {
  safeRemove(localStorage, ACCESS_TOKEN_KEY);
  safeRemove(localStorage, REFRESH_TOKEN_KEY);
  safeRemove(sessionStorage, ACCESS_TOKEN_KEY);
  safeRemove(sessionStorage, REFRESH_TOKEN_KEY);
  safeRemove(localStorage, TOKEN_STORAGE_MODE_KEY);
}
