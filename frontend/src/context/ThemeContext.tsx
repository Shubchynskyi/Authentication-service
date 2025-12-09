import React, { createContext, useContext, useState, useCallback } from 'react';
import { ThemeProvider as MuiThemeProvider, createTheme, Theme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

interface ThemeContextType {
    isDarkMode: boolean;
    toggleTheme: () => void;
    theme: Theme;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isDarkMode, setIsDarkMode] = useState(() => {
        const savedTheme = localStorage.getItem('theme');
        return savedTheme ? savedTheme === 'dark' : false;
    });

    const primaryMain = isDarkMode ? '#80cbc4' : '#1976d2';
    const secondaryMain = isDarkMode ? '#f48fb1' : '#dc004e';

    const theme = createTheme({
        palette: {
            mode: isDarkMode ? 'dark' : 'light',
            primary: {
                main: primaryMain,
            },
            secondary: {
                main: secondaryMain,
            },
            background: {
                default: isDarkMode ? '#303030' : '#f5f5f5',
                paper: isDarkMode ? '#424242' : '#ffffff',
            },
        },
        components: {
            MuiAppBar: {
                styleOverrides: {
                    root: {
                        backgroundColor: isDarkMode ? '#424242' : '#ffffff',
                        color: isDarkMode ? '#ffffff' : '#000000',
                    },
                },
            },
            MuiLink: {
                styleOverrides: {
                    root: {
                        color: primaryMain,
                    },
                },
            },
        },
    });

    const toggleTheme = useCallback(() => {
        setIsDarkMode(prev => {
            const newMode = !prev;
            localStorage.setItem('theme', newMode ? 'dark' : 'light');
            return newMode;
        });
    }, []);

    return (
        <ThemeContext.Provider value={{ isDarkMode, toggleTheme, theme }}>
            <MuiThemeProvider theme={theme}>
                <CssBaseline />
                {children}
            </MuiThemeProvider>
        </ThemeContext.Provider>
    );
};

export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (context === undefined) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
}; 