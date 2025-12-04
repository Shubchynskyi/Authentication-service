import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { ThemeProvider, useTheme } from './ThemeContext';
import { setupTestCleanup } from '../test-utils/test-helpers';

// Test component that uses ThemeContext
const TestComponent: React.FC = () => {
    const { isDarkMode, toggleTheme, theme } = useTheme();

    return (
        <div>
            <div data-testid="isDarkMode">{isDarkMode ? 'true' : 'false'}</div>
            <div data-testid="theme-mode">{theme.palette.mode}</div>
            <div data-testid="theme-background-default">{theme.palette.background.default}</div>
            <div data-testid="theme-background-paper">{theme.palette.background.paper}</div>
            <button data-testid="toggle-theme" onClick={toggleTheme}>
                Toggle Theme
            </button>
        </div>
    );
};

describe('ThemeContext', () => {
    setupTestCleanup();

    describe('Initialization', () => {
        it('should initialize with light mode when no saved theme exists', () => {
            localStorage.removeItem('theme');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('false');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('light');
        });

        it('should initialize with dark mode when saved theme is dark', () => {
            localStorage.setItem('theme', 'dark');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('dark');
        });

        it('should initialize with light mode when saved theme is light', () => {
            localStorage.setItem('theme', 'light');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('false');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('light');
        });

        it('should initialize with light mode when saved theme is invalid', () => {
            localStorage.setItem('theme', 'invalid');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('false');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('light');
        });
    });

    describe('toggleTheme', () => {
        it('should toggle from light to dark mode', () => {
            localStorage.setItem('theme', 'light');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('false');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('light');

            const toggleButton = screen.getByTestId('toggle-theme');
            fireEvent.click(toggleButton);

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('dark');
            expect(localStorage.getItem('theme')).toBe('dark');
        });

        it('should toggle from dark to light mode', () => {
            localStorage.setItem('theme', 'dark');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('dark');

            const toggleButton = screen.getByTestId('toggle-theme');
            fireEvent.click(toggleButton);

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('false');
            expect(screen.getByTestId('theme-mode')).toHaveTextContent('light');
            expect(localStorage.getItem('theme')).toBe('light');
        });

        it('should save theme to localStorage when toggling', () => {
            localStorage.removeItem('theme');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            const toggleButton = screen.getByTestId('toggle-theme');
            fireEvent.click(toggleButton);

            expect(localStorage.getItem('theme')).toBe('dark');

            fireEvent.click(toggleButton);

            expect(localStorage.getItem('theme')).toBe('light');
        });

        it('should update theme object when toggling', () => {
            localStorage.setItem('theme', 'light');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('theme-background-default')).toHaveTextContent('#f5f5f5');
            expect(screen.getByTestId('theme-background-paper')).toHaveTextContent('#ffffff');

            const toggleButton = screen.getByTestId('toggle-theme');
            fireEvent.click(toggleButton);

            expect(screen.getByTestId('theme-background-default')).toHaveTextContent('#303030');
            expect(screen.getByTestId('theme-background-paper')).toHaveTextContent('#424242');
        });

        it('should allow multiple toggles', () => {
            localStorage.setItem('theme', 'light');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            const toggleButton = screen.getByTestId('toggle-theme');

            // Toggle 1: light -> dark
            fireEvent.click(toggleButton);
            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');
            expect(localStorage.getItem('theme')).toBe('dark');

            // Toggle 2: dark -> light
            fireEvent.click(toggleButton);
            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('false');
            expect(localStorage.getItem('theme')).toBe('light');

            // Toggle 3: light -> dark
            fireEvent.click(toggleButton);
            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');
            expect(localStorage.getItem('theme')).toBe('dark');
        });
    });

    describe('Theme configuration', () => {
        it('should have correct light theme colors', () => {
            localStorage.setItem('theme', 'light');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('theme-mode')).toHaveTextContent('light');
            expect(screen.getByTestId('theme-background-default')).toHaveTextContent('#f5f5f5');
            expect(screen.getByTestId('theme-background-paper')).toHaveTextContent('#ffffff');
        });

        it('should have correct dark theme colors', () => {
            localStorage.setItem('theme', 'dark');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('theme-mode')).toHaveTextContent('dark');
            expect(screen.getByTestId('theme-background-default')).toHaveTextContent('#303030');
            expect(screen.getByTestId('theme-background-paper')).toHaveTextContent('#424242');
        });

        it('should have primary and secondary colors', () => {
            const TestThemeComponent = () => {
                const { theme } = useTheme();
                return (
                    <div>
                        <div data-testid="primary-color">{theme.palette.primary.main}</div>
                        <div data-testid="secondary-color">{theme.palette.secondary.main}</div>
                    </div>
                );
            };

            render(
                <ThemeProvider>
                    <TestThemeComponent />
                </ThemeProvider>
            );

            // Check that primary and secondary colors are set
            expect(screen.getByTestId('primary-color')).toHaveTextContent('#1976d2');
            expect(screen.getByTestId('secondary-color')).toHaveTextContent('#dc004e');
        });
    });

    describe('useTheme hook', () => {
        it('should throw error when used outside ThemeProvider', () => {
            // Suppress console.error for this test
            const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            const TestComponentWithoutProvider = () => {
                useTheme();
                return <div>Test</div>;
            };

            expect(() => {
                render(<TestComponentWithoutProvider />);
            }).toThrow('useTheme must be used within a ThemeProvider');

            consoleSpy.mockRestore();
        });
    });

    describe('Persistence', () => {
        it('should persist theme preference across re-renders', () => {
            localStorage.setItem('theme', 'dark');

            const { rerender } = render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');

            rerender(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(screen.getByTestId('isDarkMode')).toHaveTextContent('true');
            expect(localStorage.getItem('theme')).toBe('dark');
        });

        it('should update localStorage immediately on toggle', () => {
            localStorage.setItem('theme', 'light');

            render(
                <ThemeProvider>
                    <TestComponent />
                </ThemeProvider>
            );

            expect(localStorage.getItem('theme')).toBe('light');

            const toggleButton = screen.getByTestId('toggle-theme');
            fireEvent.click(toggleButton);

            // Should be updated immediately
            expect(localStorage.getItem('theme')).toBe('dark');
        });
    });
});

