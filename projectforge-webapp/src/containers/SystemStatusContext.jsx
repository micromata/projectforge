import React from 'react';

export const systemStatusContextDefaultValues = {
    /**
     * @type {String} The name of the app.
     */
    appName: 'ProjectForge',
    /**
     * @type {String} Current installed version.
     */
    version: undefined,
    /**
     * @type {String} Timestamp when the release was built.
     */
    releaseTimestamp: undefined,
    /**
     * @type {String} Date when the release was published.
     */
    releaseDate: undefined,
    /**
     * @type {String} Message of the day for the login screen.
     */
    messageOfTheDay: undefined,
    /**
     * @type {String} Url where the logo can be found if one is set.
     */
    logoUrl: undefined,
};

// The context to access the data from the rsPublic/systemStatus call.
export const SystemStatusContext = React.createContext(systemStatusContextDefaultValues);
