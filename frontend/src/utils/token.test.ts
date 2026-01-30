import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import {
  isValidJwtFormat,
  isJwtExpired,
  getAccessToken,
  setTokens,
  clearTokens,
} from './token';

describe('token utilities', () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    clearTokens();
  });

  describe('isValidJwtFormat', () => {
    it('returns true for valid JWT format', () => {
      const validToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c';
      expect(isValidJwtFormat(validToken)).toBe(true);
    });

    it('returns false for null', () => {
      expect(isValidJwtFormat(null)).toBe(false);
    });

    it('returns false for undefined', () => {
      expect(isValidJwtFormat(undefined)).toBe(false);
    });

    it('returns false for empty string', () => {
      expect(isValidJwtFormat('')).toBe(false);
    });

    it('returns false for token with less than 3 parts', () => {
      expect(isValidJwtFormat('part1.part2')).toBe(false);
      expect(isValidJwtFormat('part1')).toBe(false);
    });

    it('returns false for token with more than 3 parts', () => {
      expect(isValidJwtFormat('part1.part2.part3.part4')).toBe(false);
    });

    it('returns false for token with empty parts', () => {
      expect(isValidJwtFormat('part1..part3')).toBe(false);
      expect(isValidJwtFormat('.part2.part3')).toBe(false);
      expect(isValidJwtFormat('part1.part2.')).toBe(false);
    });

    it('returns false for non-string values', () => {
      expect(isValidJwtFormat(123 as any)).toBe(false);
      expect(isValidJwtFormat({} as any)).toBe(false);
      expect(isValidJwtFormat([] as any)).toBe(false);
    });
  });

  describe('isJwtExpired', () => {
    it('returns true for null token', () => {
      expect(isJwtExpired(null)).toBe(true);
    });

    it('returns true for undefined token', () => {
      expect(isJwtExpired(undefined)).toBe(true);
    });

    it('returns true for empty string', () => {
      expect(isJwtExpired('')).toBe(true);
    });

    it('returns true for invalid JWT format', () => {
      expect(isJwtExpired('invalid.token')).toBe(true);
    });

    it('returns true for expired token', () => {
      // Token with exp in the past (exp: 1000000000 = 2001-09-09)
      const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjEwMDAwMDAwMDB9.signature';
      expect(isJwtExpired(expiredToken)).toBe(true);
    });

    it('returns false for token with future expiration', () => {
      // Token with exp in the future (exp: 9999999999 = 2286-11-20)
      const futureExp = Math.floor(Date.now() / 1000) + 3600; // 1 hour from now
      const payload = btoa(JSON.stringify({ exp: futureExp }));
      const futureToken = `header.${payload}.signature`;
      expect(isJwtExpired(futureToken)).toBe(false);
    });

    it('returns true for token without exp claim', () => {
      const payload = btoa(JSON.stringify({ sub: 'user123' }));
      const tokenWithoutExp = `header.${payload}.signature`;
      expect(isJwtExpired(tokenWithoutExp)).toBe(true);
    });

    it('returns true for token with invalid exp format', () => {
      const payload = btoa(JSON.stringify({ exp: 'invalid' }));
      const tokenWithInvalidExp = `header.${payload}.signature`;
      expect(isJwtExpired(tokenWithInvalidExp)).toBe(true);
    });
  });

  describe('getAccessToken', () => {
    it('returns null when token is not set', () => {
      expect(getAccessToken()).toBeNull();
    });

    it('returns token from memory', () => {
      const token = 'test-access-token';
      setTokens(token);
      expect(getAccessToken()).toBe(token);
    });
  });

  describe('clearTokens', () => {
    it('removes access token from memory', () => {
      setTokens('test-access');

      clearTokens();

      expect(getAccessToken()).toBeNull();
    });

    it('does not throw when tokens do not exist', () => {
      expect(() => clearTokens()).not.toThrow();
    });
  });
});

