import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';
import AdminSection from './AdminSection';

// Mock ProfileContext
const { mockUseProfile } = vi.hoisted(() => {
    const mockUseProfile = vi.fn();
    return { mockUseProfile };
});

vi.mock('../context/ProfileContext', () => ({
    useProfile: () => mockUseProfile(),
}));

describe('AdminSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Reset mock to ensure clean state between tests
        mockUseProfile.mockReset();
        mockUseProfile.mockClear();
        // Set default return value
        mockUseProfile.mockReturnValue({
            isAdmin: false,
            isLoading: false,
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
        mockUseProfile.mockReset();
        mockUseProfile.mockClear();
    });

    it('renders children when user is admin', async () => {
        mockUseProfile.mockReturnValue({
            isAdmin: true,
            isLoading: false,
        });

        render(
            <AdminSection>
                <div>Admin Content</div>
            </AdminSection>
        );

        // Use findByText for async rendering
        const content = await screen.findByText('Admin Content', {}, { timeout: 5000 });
        expect(content).toBeInTheDocument();
    });

    it('does not render children when user is not admin', () => {
        mockUseProfile.mockReturnValue({
            isAdmin: false,
            isLoading: false,
        });

        const { container } = render(
            <AdminSection>
                <div>Admin Content</div>
            </AdminSection>
        );

        expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
        expect(container.firstChild).toBeNull();
    });

    it('shows loading state when profile is loading', () => {
        mockUseProfile.mockReturnValue({
            isAdmin: false,
            isLoading: true,
        });

        render(
            <AdminSection>
                <div>Admin Content</div>
            </AdminSection>
        );

        // Should show CircularProgress
        expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
    });

    it('renders multiple children when user is admin', async () => {
        mockUseProfile.mockReturnValue({
            isAdmin: true,
            isLoading: false,
        });

        render(
            <AdminSection>
                <div>First Child</div>
                <div>Second Child</div>
            </AdminSection>
        );

        // Use findByText for async rendering
        const firstChild = await screen.findByText('First Child', {}, { timeout: 5000 });
        expect(firstChild).toBeInTheDocument();
        
        const secondChild = await screen.findByText('Second Child', {}, { timeout: 5000 });
        expect(secondChild).toBeInTheDocument();
    });
});

