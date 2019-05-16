import React from 'react';

export const defaultValues = {
    ui: {},
    options: {
        displayPageMenu: true,
        setBrowserTitle: true,
        showPageMenuTitle: true,
    },
    renderLayout: () => <React.Fragment />,
};


export const DynamicLayoutContext = React.createContext(defaultValues);
