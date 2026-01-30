import { beforeEach, describe, expect, it, vi } from 'vitest';

const STORAGE_KEY = 'auth-event';

describe('authEvents storage fallback', () => {
    beforeEach(() => {
        vi.resetModules();
        vi.unstubAllGlobals();
        localStorage.clear();
        vi.stubGlobal('BroadcastChannel', undefined as any);
    });

    it('stores event in localStorage when BroadcastChannel is unavailable', async () => {
        const { broadcastAuthEvent } = await import('./authEvents');

        broadcastAuthEvent('logout');

        const raw = localStorage.getItem(STORAGE_KEY);
        expect(raw).toContain('"logout"');
    });

    it('delivers storage events to subscriber', async () => {
        const { subscribeAuthEvents } = await import('./authEvents');
        const handler = vi.fn();

        const unsubscribe = subscribeAuthEvents(handler);
        const payload = { type: 'login', at: 123 };
        window.dispatchEvent(
            new StorageEvent('storage', {
                key: STORAGE_KEY,
                newValue: JSON.stringify(payload),
            })
        );

        expect(handler).toHaveBeenCalledWith(payload);
        unsubscribe();
    });
});

describe('authEvents BroadcastChannel', () => {
    beforeEach(() => {
        vi.resetModules();
        vi.unstubAllGlobals();
        localStorage.clear();
    });

    it('uses BroadcastChannel when available', async () => {
        class MockBroadcastChannel {
            private listeners: Array<(event: MessageEvent) => void> = [];
            public name: string;

            constructor(name: string) {
                this.name = name;
            }

            postMessage = (data: any) => {
                this.listeners.forEach((listener) => listener({ data } as MessageEvent));
            };

            addEventListener = (type: string, listener: (event: MessageEvent) => void) => {
                if (type === 'message') {
                    this.listeners.push(listener);
                }
            };

            removeEventListener = (type: string, listener: (event: MessageEvent) => void) => {
                if (type === 'message') {
                    this.listeners = this.listeners.filter((item) => item !== listener);
                }
            };
        }

        vi.stubGlobal('BroadcastChannel', MockBroadcastChannel as any);

        const { broadcastAuthEvent, subscribeAuthEvents } = await import('./authEvents');
        const handler = vi.fn();

        const unsubscribe = subscribeAuthEvents(handler);
        broadcastAuthEvent('login');

        expect(handler).toHaveBeenCalledWith(expect.objectContaining({ type: 'login' }));
        unsubscribe();
    });
});
