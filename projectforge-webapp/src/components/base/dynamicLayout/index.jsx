import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import {
    defaultValues as dynamicLayoutContextDefaultValues,
    DynamicLayoutContext,
} from './context';
import renderLayout from './components/DynamicRenderer';
import DynamicPageMenu from './DynamicPageMenu';

function DynamicLayout({ ui, options, ...props }) {
    // Destructure the 'ui' prop.
    const {
        title,
        pageMenu,
    } = ui;

    const {
        setBrowserTitle,
        displayPageMenu,
    } = options;

    // Set the document title when a title for the page is specified and the option
    // setBrowserTitle is true.
    if (setBrowserTitle && title) {
        document.title = `ProjectForge - ${title}`;
    }

    return (
        <DynamicLayoutContext.Provider
            value={{
                ...dynamicLayoutContextDefaultValues,
                ui,
                options,
                renderLayout,
                ...props,
            }}
        >
            {/* Render Page Menu if the option displayPageMenu is true. */
                displayPageMenu
                    ? <DynamicPageMenu menu={pageMenu} title={title} />
                    : undefined}
            {renderLayout(ui.layout)}
        </DynamicLayoutContext.Provider>
    );
}

DynamicLayout.propTypes = {
    // UI Prop
    ui: PropTypes.shape({
        title: PropTypes.string,
        pageMenu: PropTypes.arrayOf(menuItemPropType),
    }).isRequired,
    // Customization options
    options: PropTypes.shape({
        displayPageMenu: PropTypes.bool,
        setBrowserTitle: PropTypes.bool,
        showPageMenuTitle: PropTypes.bool,
    }),
};

DynamicLayout.defaultProps = {
    options: dynamicLayoutContextDefaultValues.options,
};

export default DynamicLayout;
