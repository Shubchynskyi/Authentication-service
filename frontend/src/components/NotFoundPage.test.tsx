import { screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import NotFoundPage from './NotFoundPage';
import { renderWithRouter, setupTestCleanup } from '../test-utils/test-helpers';

describe('NotFoundPage', () => {
    setupTestCleanup();

    it('renders correctly', async () => {
        renderWithRouter(<NotFoundPage />);

        // Use findByText which automatically waits for async rendering
        const title = await screen.findByText('Page Not Found', {}, { timeout: 5000 });
        expect(title).toBeInTheDocument();
        
        const description = await screen.findByText(/The page you are looking for does not exist/i, {}, { timeout: 5000 });
        expect(description).toBeInTheDocument();
        
        const backButton = await screen.findByText('Back to Home', {}, { timeout: 5000 });
        expect(backButton).toBeInTheDocument();
    });

    it('has a link to home page', async () => {
        renderWithRouter(<NotFoundPage />);

        // Use findByRole which automatically waits for async rendering
        const link = await screen.findByRole('link', { name: /Back to Home/i }, { timeout: 5000 });
        expect(link).toHaveAttribute('href', '/');
    });
});
