import { render, screen, fireEvent } from '@testing-library/react';
import { vi, afterEach, describe, it, expect } from 'vitest';
import LanguageSwitcher from './LanguageSwitcher';

describe('LanguageSwitcher', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    it('renders correctly', () => {
        render(<LanguageSwitcher />);
        expect(screen.getByLabelText('Language')).toBeInTheDocument();
    });

    it('displays available languages', () => {
        render(<LanguageSwitcher />);

        const select = screen.getByRole('combobox');
        fireEvent.mouseDown(select);

        const options = screen.getAllByRole('option');
        expect(options.length).toBeGreaterThan(0);
    });
});
