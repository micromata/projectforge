import React from 'react';

export const defaultValues = {
    /**
     * Overrides the calendar data with the changed from the REST Call.
     *
     * @params {Object} The json from the server call.
     */
    // Disable no-unused-vars so its clear what you need to override tthe function.
    /* eslint-disable-next-line no-unused-vars */
    saveUpdateResponseInState: (json) => {
        throw new Error('not implemented.');
    },
};

export const CalendarContext = React.createContext(defaultValues);
