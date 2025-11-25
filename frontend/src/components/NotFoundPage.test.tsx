import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi, afterEach, describe, it, expect } from 'vitest';
import NotFoundPage from './NotFoundPage';

describe('NotFoundPage', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders correctly', () => {
        render(
            <BrowserRouter>
                <NotFoundPage />
            </BrowserRouter>
        );

        expect(screen.getByText('Page Not Found')).toBeInTheDocument();
        expect(screen.getByText(/The page you are looking for does not exist/i)).toBeInTheDocument();
        expect(screen.getByText('Back to Home')).toBeInTheDocument();
    });

    it('has a link to home page', () => {
        render(
            <BrowserRouter>
                <NotFoundPage />
            </BrowserRouter>
        );

        const link = screen.getByRole('link', { name: /Back to Home/i });
        expect(link).toHaveAttribute('href', '/');
    });
});
