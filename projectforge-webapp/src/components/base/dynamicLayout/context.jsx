import React from 'react';

// Default values for the context. Everything you can use, should be listed here.
export const defaultValues = {
    /**
     * Call actions delivered in ui by the rest call.
     *
     * @param {Object} action The action object, delivered by the rest api (ui).
     */
    // Disable no-unused-vars so its clear what you need to override the function.
    /* eslint-disable-next-line no-unused-vars */
    callAction: (action) => {
        throw Error('not implemented yet.');
    },
    /**
     * @type {Object} Stores data for input fields etc. Can be changed via setData().
     */
    data: {},
    /**
     * @type {Boolean} Is the current layout in a fetching state.
     */
    isFetching: false,
    /**
     * @type {Object} Specify settings for some general layout helpers.
     */
    options: {
        /**
         * @type {Boolean} Disable render while loading or other circumstances.
         */
        disableLayoutRendering: false,
        /**
         * @type {Boolean} Enables a general page menu.
         */
        displayPageMenu: true,
        /**
         * @type {Boolean} Changes the title of the browser to the title of the layout.
         */
        setBrowserTitle: true,
        /**
         *  @type {Boolean} Shows the action group buttons on the bottom.
         */
        showActionButtons: true,
        /**
         * @type {Boolean} Shows the layout title in the page menu.
         */
        showPageMenuTitle: true,
    },
    /**
     * Renders the layout out of an array of dynamic layout objects.
     *
     * @param {Array<Object>} content Array of dynamic content objects.
     *  @see propTypes.contentPropType
     *
     * @returns {Node} The node that should be rendered in render() function of a React component.
     */
    // Disable no-unused-vars so its clear what you need to override the function.
    /* eslint-disable-next-line no-unused-vars */
    renderLayout: (content) => <></>,
    /**
     * Modifies the context data object in a way like Reacts setState().
     *
     * Note: That it only touches the values specified in newData!
     *
     * @param {(Object|function(Object):Object)} newData The new data as an object or an function
     *  that accept the current data and calculates the new data.
     * @param {function(Object):void=} callback The callback function will be called when the new
     *  Data was set.
     *
     * @returns {Promise<Object>} If you want to use the promise architecture instead of a callback
     *  function you can go with the returned promise.
     */
    // Disable no-unused-vars so its clear what you need to override the function.
    /* eslint-disable-next-line no-unused-vars */
    setData: async (newData, callback) => {
        throw new Error('not implemented yet');
    },
    /**
     * Modifies the context variables object in a way like Reacts setState()
     *
     * Note: Only changes the values specified in newVariables!
     *
     * @param {(Object|function(Object):Object)} newVariables The new variables as an object or an
     *  function that accept the current data and calculates the new variables.
     * @param {function(Object):void=} callback The callback function will be called then the new
     *  variables were set.
     * @returns {Promise<Object>} If you want to use the promise architecture instead of a callback
     *  function you can go with the returned promise.
     */
    // Disable no-unused-vars so its clear what you need to override the function.
    /* eslint-disable-next-line no-unused-vars */
    setVariables: async (newVariables, callback) => {
        throw new Error('not implemented yet');
    },
    /**
     * @type {Object} The ui object sent by the server. @see UILayout.kt
     */
    ui: {
        /**
         * @type {Object} The sent translations in key -> value format.
         */
        translations: {},
    },
    /**
     * @type {Array<Object>} The validationErrors sent by the server. Not always present.
     */
    validationErrors: [],
    /**
     * @type {Object} The variables object sent by the server. @see AbstractBaseRest.kt >
     *  EditLayoutData. Only sometimes present.
     */
    variables: {},
};

// The context to access dynamic layout related variables in the dynamic layout system.
export const DynamicLayoutContext = React.createContext(defaultValues);
