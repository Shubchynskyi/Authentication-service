
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import MaskedLoginTemplate from './MaskedLoginTemplate';
import { LayoutProvider } from '../context/LayoutContext';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
    useNavigate: () => mockNavigate,
}));

// Wrapper component with LayoutProvider
const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    return <LayoutProvider>{children}</LayoutProvider>;
};

describe('MaskedLoginTemplate', () => {
    let originalLocation: Location;

    beforeEach(() => {
        vi.clearAllMocks();
        // Mock window.location to ensure origin is available
        originalLocation = window.location;
        delete (window as any).location;
        (window as any).location = {
            origin: 'http://localhost:3000',
            href: 'http://localhost:3000',
        };
    });

    afterEach(() => {
        // Restore window.location
        (window as any).location = originalLocation;
    });


    it('should render HTML content correctly', () => {
        const htmlContent = '<div data-testid="content">Test Content</div>';
        render(
            <TestWrapper>
                <MaskedLoginTemplate htmlContent={htmlContent} />
            </TestWrapper>
        );

        expect(screen.getByTestId('content')).toBeInTheDocument();
        expect(screen.getByText('Test Content')).toBeInTheDocument();
    });

    it('should handle navigation when clicking link with secret parameter', () => {
        const htmlContent = `
            <div>
                <a href="/login?secret=true" data-testid="secret-link">Secret Link</a>
            </div>
        `;
        render(
            <TestWrapper>
                <MaskedLoginTemplate htmlContent={htmlContent} />
            </TestWrapper>
        );

        const link = screen.getByTestId('secret-link');
        fireEvent.click(link);

        expect(mockNavigate).toHaveBeenCalledWith('/login?secret=true');
    });

    it('should handle navigation for register link with secret parameter', () => {
        const htmlContent = `
            <div>
                <a href="/register?secret=true" data-testid="register-link">Register Link</a>
            </div>
        `;
        render(
            <TestWrapper>
                <MaskedLoginTemplate htmlContent={htmlContent} />
            </TestWrapper>
        );

        const link = screen.getByTestId('register-link');
        fireEvent.click(link);

        expect(mockNavigate).toHaveBeenCalledWith('/register?secret=true');
    });

    it('should not navigate when clicking normal link', () => {
        const htmlContent = `
            <div>
                <a href="/other" data-testid="normal-link">Normal Link</a>
            </div>
        `;
        render(
            <TestWrapper>
                <MaskedLoginTemplate htmlContent={htmlContent} />
            </TestWrapper>
        );

        const link = screen.getByTestId('normal-link');
        fireEvent.click(link);

        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should ignore clicks on non-link elements', () => {
        const htmlContent = `
            <div data-testid="container">
                <span>Not a link</span>
            </div>
        `;
        render(
            <TestWrapper>
                <MaskedLoginTemplate htmlContent={htmlContent} />
            </TestWrapper>
        );

        const container = screen.getByTestId('container');
        fireEvent.click(container);

        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should handle nested elements in secret link', () => {
        const htmlContent = `
            <div>
                <a href="/login?secret=true" data-testid="nested-link">
                    <span>Click me</span>
                </a>
            </div>
        `;
        render(
            <TestWrapper>
                <MaskedLoginTemplate htmlContent={htmlContent} />
            </TestWrapper>
        );

        const span = screen.getByText('Click me');
        fireEvent.click(span);

        expect(mockNavigate).toHaveBeenCalledWith('/login?secret=true');
    });
});
