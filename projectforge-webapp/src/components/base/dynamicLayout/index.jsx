import PropTypes from 'prop-types';
import React from 'react';
import { buttonPropType, menuItemPropType } from '../../../utilities/propTypes';
import ActionGroup from '../page/action/Group';
import renderLayout from './components/DynamicRenderer';
import {
    defaultValues as dynamicLayoutContextDefaultValues,
    DynamicLayoutContext,
} from './context';
import DynamicPageMenu from './DynamicPageMenu';

function DynamicLayout(
    {
        children,
        ui,
        options,
        ...props
    },
) {
    // Destructure the 'ui' prop.
    const {
        actions,
        layout,
        title,
        pageMenu,
    } = ui;

    const {
        displayPageMenu,
        setBrowserTitle,
        showActionButtons,
    } = options;

    // Set the document title when a title for the page is specified and the option
    // setBrowserTitle is true.
    React.useEffect(() => {
        if (setBrowserTitle && title) {
            document.title = `ProjectForge - ${title}`;
        }
    }, [setBrowserTitle, title]);

    // Render PageMenu if the option displayPageMenu is true.
    const menu = React.useMemo(() => (
        <React.Fragment>
            {displayPageMenu
                ? <DynamicPageMenu menu={pageMenu} title={title} />
                : undefined}
        </React.Fragment>
    ), [displayPageMenu, pageMenu, title]);

    // Render ActionGroup if actions were found in the ui object.
    const actionGroup = React.useMemo(() => (
        <React.Fragment>
            {actions && showActionButtons
                ? <ActionGroup actions={actions} />
                : undefined}
        </React.Fragment>
    ), [actions, showActionButtons]);

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
            {menu}
            {children}
            {renderLayout(layout)}
            {actionGroup}
        </DynamicLayoutContext.Provider>
    );
}

DynamicLayout.propTypes = {
    // UI Prop
    ui: PropTypes.shape({
        actions: PropTypes.arrayOf(buttonPropType),
        layout: PropTypes.array,
        title: PropTypes.string,
        pageMenu: PropTypes.arrayOf(menuItemPropType),
    }).isRequired,
    // Additional content to be displayed in the DynamicLayout context.
    children: PropTypes.node,
    // Customization options
    options: PropTypes.shape({
        displayPageMenu: PropTypes.bool,
        setBrowserTitle: PropTypes.bool,
        showActionButtons: PropTypes.bool,
        showPageMenuTitle: PropTypes.bool,
    }),
};

DynamicLayout.defaultProps = {
    children: undefined,
    options: dynamicLayoutContextDefaultValues.options,
};

export default DynamicLayout;
