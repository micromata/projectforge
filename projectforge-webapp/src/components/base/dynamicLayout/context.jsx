import React from 'react';

// Default values for the context. Everything you can use, should be listed here.
export const defaultValues = {
    ui: {},
    options: {
        displayPageMenu: true,
        setBrowserTitle: true,
        showPageMenuTitle: true,
    },
    renderLayout: () => <React.Fragment />,
};

// The context to access dynamic layout related variables in the dynamic layout system.
export const DynamicLayoutContext = React.createContext(defaultValues);
