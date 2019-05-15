import React from 'react';

export const defaultValues = {
    options: {
        displayPageMenu: true,
        setBrowserTitle: true,
        showPageMenuTitle: true,
    },
};


export const DynamicLayoutContext = React.createContext(defaultValues);
