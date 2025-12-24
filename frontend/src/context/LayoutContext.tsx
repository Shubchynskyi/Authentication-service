import React, { createContext, useContext, useState, ReactNode } from 'react';

interface LayoutContextType {
    hideNavbar: boolean;
    setHideNavbar: (hide: boolean) => void;
}

const LayoutContext = createContext<LayoutContextType | undefined>(undefined);

export const LayoutProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [hideNavbar, setHideNavbar] = useState(false);

    return (
        <LayoutContext.Provider value={{ hideNavbar, setHideNavbar }}>
            {children}
        </LayoutContext.Provider>
    );
};

export const useLayout = (): LayoutContextType => {
    const context = useContext(LayoutContext);
    if (!context) {
        throw new Error('useLayout must be used within a LayoutProvider');
    }
    return context;
};

