import React from 'react';
import { BrowserRouter, MemoryRouter, MemoryRouterProps } from 'react-router-dom';

// Router components with future flags to suppress warnings
export const TestBrowserRouter: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      {children}
    </BrowserRouter>
  );
};

export const TestMemoryRouter: React.FC<MemoryRouterProps> = ({ children, ...props }) => {
  return (
    <MemoryRouter 
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      {...props}
    >
      {children}
    </MemoryRouter>
  );
};

