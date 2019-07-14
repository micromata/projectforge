import React from 'react';

const notImplementedFunction = () => {
    throw Error('Not implemented.');
};

export const defaultValues = {
    /**
     *  @type {String} Current category of the list page.
     */
    category: '',
    /**
     * @type {Object} Stores the current filter. Can be changed via setFilter() or the filterHelper.
     */
    filter: {
        /**
         * @type {Array<Object>} Contains the magic filter entries from the input field.
         */
        entries: [],
        /**
         * @type {Object} Map of the checkboxes and maxRows.
         */
        searchFilter: {},
    },
    filterFavorites: [],
    /**
     * @type {Object} Contains some functions that support the filter manipulation.
     */
    filterHelper: {
        /**
         * Add a new entry to the filter entries.
         *
         * @param {Object} entry The entry from ReactSelect to be added.
         */
        // Disable no-unused-vars so its clear what you need to override the function.
        /* eslint-disable-next-line no-unused-vars */
        addEntry: entry => notImplementedFunction(),
        /**
         * Clears all entries.
         */
        clearEntries: () => notImplementedFunction(),
        /**
         * Edit a certain entry.
         *
         * @param {String} id The id of the entry.
         * @param {Any} newValue The new value of the entry.
         */
        // Disable no-unused-vars so its clear what you need to override the function.
        /* eslint-disable-next-line no-unused-vars */
        editEntry: (id, newValue) => notImplementedFunction(),
        /**
         * Remove an entry from the filter entries.
         *
         * @param {String} fieldOrSearch The fieldId or the search from a created pill.
         */
        // Disable no-unused-vars so its clear what you need to override the function.
        /* eslint-disable-next-line no-unused-vars */
        removeEntry: fieldOrSearch => notImplementedFunction(),
        /**
         * Sets a specific extended filter.
         * @param {String} id The id of the extended filter.
         * @param {Any} value The new value of the extended filter.
         */
        // Disable no-unused-vars so its clear what you need to override the function.
        /* eslint-disable-next-line no-unused-vars */
        setExtended: (id, value) => notImplementedFunction(),
        /**
         * Sets a specific filter.
         * @param {String} id The id of the filter like maxRows.
         * @param {Any} value The new value of the filter.
         */
        // Disable no-unused-vars so its clear what you need to override the function.
        /* eslint-disable-next-line no-unused-vars */
        setFilter: (id, value) => notImplementedFunction(),
        /**
         * Sets the filter state.
         *
         * @param {Object} filter New filter. @see defaultValues.filter
         */
        // Disable no-unused-vars so its clear what you need to override the function.
        /* eslint-disable-next-line no-unused-vars */
        setFilterState: filter => notImplementedFunction(),
    },
    /**
     * Set the filter favorites.
     * @param {Object} filterFavorites The filter favorites provided by the rest api.
     */
    // Disable no-unused-vars so its clear what you need to override the function.
    /* eslint-disable-next-line no-unused-vars */
    setFilterFavorites: filterFavorites => notImplementedFunction(),
    /**
     * Set the ui object. Needed for favorite handling.
     * @param {Object} ui The ui object provided by rest. See DynamicLayoutContext.ui
     */
    // Disable no-unused-vars so its clear what you need to override the function.
    /* eslint-disable-next-line no-unused-vars */
    setUI: ui => notImplementedFunction(),
};

export const ListPageContext = React.createContext(defaultValues);
