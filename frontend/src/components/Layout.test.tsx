import { screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import Layout from './Layout';
import { renderWithRouter, setupTestCleanup } from '../test-utils/test-helpers';

// Mock LanguageSwitcher
vi.mock('./LanguageSwitcher', () => ({
    default: () => <div>LanguageSwitcher</div>,
}));

describe('Layout', () => {
    setupTestCleanup();

    it('renders children', async () => {
        renderWithRouter(
            <Layout>
                <div>Test Content</div>
            </Layout>
        );

        await waitFor(() => {
            expect(screen.getByText('Test Content')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('renders with default title', async () => {
        renderWithRouter(
            <Layout>
                <div>Content</div>
            </Layout>
        );

        // Check if welcome text is in the Typography (it's in AppBar, not a heading)
        // There are multiple "Welcome" elements, so use getAllByText
        await waitFor(() => {
            const welcomeTexts = screen.getAllByText('Welcome');
            expect(welcomeTexts.length).toBeGreaterThan(0);
        }, { timeout: 5000 });
    });

    it('renders with custom title', async () => {
        renderWithRouter(
            <Layout title="Custom Title">
                <div>Content</div>
            </Layout>
        );

        await waitFor(() => {
            expect(screen.getByText('Custom Title')).toBeInTheDocument();
        }, { timeout: 5000 });
    });

    it('renders navigation links', async () => {
        renderWithRouter(
            <Layout>
                <div>Content</div>
            </Layout>
        );

        const welcomeLink = await screen.findByRole('link', { name: /Welcome/i }, { timeout: 5000 });
        expect(welcomeLink).toHaveAttribute('href', '/');
        
        const loginLink = await screen.findByRole('link', { name: /Sign In|Login/i }, { timeout: 5000 });
        expect(loginLink).toHaveAttribute('href', '/login');
        
        const registerLink = await screen.findByRole('link', { name: /Register/i }, { timeout: 5000 });
        expect(registerLink).toHaveAttribute('href', '/register');
    });

    it('renders language switcher', async () => {
        renderWithRouter(
            <Layout>
                <div>Content</div>
            </Layout>
        );

        await waitFor(() => {
            expect(screen.getByText('LanguageSwitcher')).toBeInTheDocument();
        }, { timeout: 5000 });
    });
});

