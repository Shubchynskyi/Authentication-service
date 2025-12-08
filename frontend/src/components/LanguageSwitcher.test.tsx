import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { vi, afterEach, beforeEach, describe, it, expect } from 'vitest';
import LanguageSwitcher from './LanguageSwitcher';

// Mock i18n
const { mockChangeLanguage, mockI18n, mockTranslations } = vi.hoisted(() => {
    const mockChangeLanguage = vi.fn();
    const mockI18n = {
        language: 'en',
        changeLanguage: mockChangeLanguage,
        resolvedLanguage: 'en',
    };
    const mockTranslations: Record<string, string> = {
        'common.language': 'Language',
    };
    return { mockChangeLanguage, mockI18n, mockTranslations };
});

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        i18n: mockI18n,
        t: (key: string) => mockTranslations[key] || key,
    }),
    initReactI18next: {
        type: '3rdParty',
        init: vi.fn(),
    },
}));

// Mock availableLanguages
vi.mock('../i18n/i18n', () => ({
    availableLanguages: {
        en: 'English',
        ru: 'Русский',
        ua: 'Українська',
        de: 'Deutsch',
    },
}));

describe('LanguageSwitcher', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        mockI18n.language = 'en';
        mockI18n.resolvedLanguage = 'en';
    });

    afterEach(() => {
        vi.clearAllMocks();
        localStorage.clear();
        cleanup();
    });

    it('renders correctly', async () => {
        render(<LanguageSwitcher />);
        await waitFor(() => {
            expect(screen.getByLabelText('Language')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('displays available languages', async () => {
        render(<LanguageSwitcher />);

        await waitFor(() => {
            const select = screen.getByRole('combobox');
            expect(select).toBeInTheDocument();
        }, { timeout: 5000 });

        const select = screen.getByRole('combobox');
        fireEvent.mouseDown(select);

        await waitFor(() => {
            const options = screen.getAllByRole('option');
            expect(options.length).toBeGreaterThan(0);
        });
    });

    it('changes language when option is selected', async () => {
        render(<LanguageSwitcher />);

        await waitFor(() => {
            const select = screen.getByRole('combobox');
            expect(select).toBeInTheDocument();
        }, { timeout: 5000 });

        const select = screen.getByRole('combobox');
        fireEvent.mouseDown(select);

        await waitFor(() => {
            const ruOption = screen.getByRole('option', { name: /Русский/i });
            expect(ruOption).toBeInTheDocument();
        });

        const ruOption = screen.getByRole('option', { name: /Русский/i });
        fireEvent.click(ruOption);

        await waitFor(() => {
            expect(mockChangeLanguage).toHaveBeenCalled();
        }, { timeout: 5000 });
    });

    it('saves selected language to localStorage', async () => {
        render(<LanguageSwitcher />);

        await waitFor(() => {
            const select = screen.getByRole('combobox');
            expect(select).toBeInTheDocument();
        }, { timeout: 5000 });

        const select = screen.getByRole('combobox');
        fireEvent.mouseDown(select);

        await waitFor(() => {
            const ruOption = screen.getByRole('option', { name: /Русский/i });
            expect(ruOption).toBeInTheDocument();
        });

        const ruOption = screen.getByRole('option', { name: /Русский/i });
        fireEvent.click(ruOption);

        await waitFor(() => {
            expect(localStorage.getItem('language')).toBeTruthy();
        }, { timeout: 5000 });
    });
});
