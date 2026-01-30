export type AuthEvent = {
    type: 'login' | 'logout';
    at: number;
};

const CHANNEL_NAME = 'auth-events';
const STORAGE_KEY = 'auth-event';

const channel = typeof BroadcastChannel !== 'undefined'
    ? new BroadcastChannel(CHANNEL_NAME)
    : null;

export const broadcastAuthEvent = (type: AuthEvent['type']): void => {
    const event: AuthEvent = { type, at: Date.now() };
    if (channel) {
        channel.postMessage(event);
        return;
    }
    if (typeof window === 'undefined') {
        return;
    }
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(event));
    } catch {
        // ignore
    }
};

export const subscribeAuthEvents = (handler: (event: AuthEvent) => void): (() => void) => {
    if (channel) {
        const listener = (message: MessageEvent<AuthEvent>) => {
            if (message?.data?.type) {
                handler(message.data);
            }
        };
        channel.addEventListener('message', listener);
        return () => channel.removeEventListener('message', listener);
    }

    if (typeof window === 'undefined') {
        return () => {};
    }

    const listener = (e: StorageEvent) => {
        if (e.key !== STORAGE_KEY || !e.newValue) {
            return;
        }
        try {
            const parsed = JSON.parse(e.newValue) as AuthEvent;
            if (parsed?.type) {
                handler(parsed);
            }
        } catch {
            // ignore
        }
    };

    window.addEventListener('storage', listener);
    return () => window.removeEventListener('storage', listener);
};
