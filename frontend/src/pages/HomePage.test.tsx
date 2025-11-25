import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { vi, afterEach, describe, it, expect } from 'vitest';
import HomePage from './HomePage';

describe('HomePage', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders correctly', () => {
        render(
            <BrowserRouter>
                <HomePage />
            </BrowserRouter>
        );

        expect(screen.getByText('Welcome')).toBeInTheDocument();
        expect(screen.getByText('Authentication Service')).toBeInTheDocument();
    });

    it('displays all navigation links', () => {
        render(
            <BrowserRouter>
                <HomePage />
            </BrowserRouter>
        );

        expect(screen.getByRole('link', { name: /Login/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Register/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Verify Email/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^Profile$/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Edit Profile/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Admin Panel/i })).toBeInTheDocument();
    });

    it('has correct links to routes', () => {
        render(
            <BrowserRouter>
                <HomePage />
            </BrowserRouter>
        );

        expect(screen.getByRole('link', { name: /Login/i })).toHaveAttribute('href', '/login');
        expect(screen.getByRole('link', { name: /Register/i })).toHaveAttribute('href', '/register');
        expect(screen.getByRole('link', { name: /Verify Email/i })).toHaveAttribute('href', '/verify');
        expect(screen.getByRole('link', { name: /^Profile$/i })).toHaveAttribute('href', '/profile');
        expect(screen.getByRole('link', { name: /Edit Profile/i })).toHaveAttribute('href', '/profile/edit');
        expect(screen.getByRole('link', { name: /Admin Panel/i })).toHaveAttribute('href', '/admin');
    });
});
