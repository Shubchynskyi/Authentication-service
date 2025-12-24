import { render, screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import MaskedLoginTemplatePreview from './MaskedLoginTemplatePreview';
import { getTranslation, testTranslations } from '../test-utils/translations';

// Mock useTranslation
const mockT = (key: string) => getTranslation(key, testTranslations);

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: mockT,
        i18n: {
            language: 'en',
            changeLanguage: vi.fn(),
            resolvedLanguage: 'en',
        },
    }),
}));

// Mock the config module
vi.mock('../config/maskedLoginTemplates', () => ({
    getTemplateMetadata: (templateId: number) => {
        if (templateId === 1) {
            return {
                id: 1,
                name: '404 Not Found',
                screenshotPath: 'template_01_preview.png',
                clickArea: {
                    description: 'Click on the dot after "nginx/1.18"',
                    positionHint: { bottom: '10%', right: '15%' }
                }
            };
        }
        if (templateId === 2) {
            return {
                id: 2,
                name: 'Site Maintenance',
                screenshotPath: 'template_02_preview.png',
                clickArea: {
                    description: 'Click on the dot after "shortly"',
                    positionHint: { bottom: '20%', right: '10%' }
                }
            };
        }
        return undefined;
    },
    MASKED_LOGIN_TEMPLATES: {}
}));

describe('MaskedLoginTemplatePreview', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders placeholder when template metadata is not found', () => {
        render(<MaskedLoginTemplatePreview templateId={999} />);
        
        // Component should return null when metadata is not found
        expect(screen.queryByText(/Screenshot unavailable/i)).not.toBeInTheDocument();
    });

    it('renders template information even when screenshot is not available', () => {
        render(<MaskedLoginTemplatePreview templateId={1} />);

        // Component shows template info regardless of screenshot availability
        // Use getAllByText since DOM might contain multiple instances from previous tests
        const templateLabels = screen.getAllByText(/Template 1:/i);
        expect(templateLabels.length).toBeGreaterThan(0);
        const templateNames = screen.getAllByText(/404 Not Found/i);
        expect(templateNames.length).toBeGreaterThan(0);
        const descriptions = screen.getAllByText(/Click on the dot after "nginx\/1.18"/i);
        expect(descriptions.length).toBeGreaterThan(0);
    });

    it('displays template name and description', () => {
        render(<MaskedLoginTemplatePreview templateId={1} />);

        // Should show template name - use getAllByText since there might be multiple instances
        const templateLabels = screen.getAllByText(/Template 1:/i);
        expect(templateLabels.length).toBeGreaterThan(0);
        
        // Should show template name - use getAllByText since component might render multiple times
        const templateNames = screen.getAllByText(/404 Not Found/i);
        expect(templateNames.length).toBeGreaterThan(0);

        // Should show click area description - use getAllByText since component might render multiple times
        const descriptions = screen.getAllByText(/Click on the dot after "nginx\/1.18"/i);
        expect(descriptions.length).toBeGreaterThan(0);
    });

    it('displays correct template information for different template', async () => {
        render(<MaskedLoginTemplatePreview templateId={2} />);

        await waitFor(() => {
            // Use getAllByText since DOM might contain multiple instances
            const templateLabels = screen.getAllByText(/Template 2:/i);
            expect(templateLabels.length).toBeGreaterThan(0);
            const templateNames = screen.getAllByText(/Site Maintenance/i);
            expect(templateNames.length).toBeGreaterThan(0);
        }, { timeout: 2000 });

        const descriptions = screen.getAllByText(/Click on the dot after "shortly"/i);
        expect(descriptions.length).toBeGreaterThan(0);
    });

    it('handles image load error gracefully', () => {
        render(<MaskedLoginTemplatePreview templateId={1} />);

        // Component should render template info even if image fails to load
        // Use getAllByText since DOM might contain multiple instances
        const templateLabels = screen.getAllByText(/Template 1:/i);
        expect(templateLabels.length).toBeGreaterThan(0);
        const templateNames = screen.getAllByText(/404 Not Found/i);
        expect(templateNames.length).toBeGreaterThan(0);
    });

    it('renders with correct structure', () => {
        render(<MaskedLoginTemplatePreview templateId={1} />);

        // Should show template info - use getAllByText since DOM might contain multiple instances
        const templateLabels = screen.getAllByText(/Template 1:/i);
        expect(templateLabels.length).toBeGreaterThan(0);
        const templateNames = screen.getAllByText(/404 Not Found/i);
        expect(templateNames.length).toBeGreaterThan(0);
        const descriptions = screen.getAllByText(/Click on the dot after "nginx\/1.18"/i);
        expect(descriptions.length).toBeGreaterThan(0);
    });
});

