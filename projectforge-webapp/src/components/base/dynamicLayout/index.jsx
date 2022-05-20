import PropTypes from 'prop-types';
import React from 'react';
import { buttonPropType, menuItemPropType } from '../../../utilities/propTypes';
import DynamicActionGroup from './action/DynamicActionGroup';
import renderLayout from './components/DynamicRenderer';
import {
    defaultValues as dynamicLayoutContextDefaultValues,
    DynamicLayoutContext,
} from './context';
import history from '../../../utilities/history';
import DynamicPageMenu from './DynamicPageMenu';
import { Button } from '../../design';

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
        historyBackButton,
    } = ui;

    const {
        disableLayoutRendering,
        displayPageMenu,
        setBrowserTitle,
        showActionButtons,
    } = options;

    const [previousTitle, setPreviousTitle] = React.useState(document.title);

    // Set the document title when a title for the page is specified and the option
    // setBrowserTitle is true.
    React.useEffect(() => {
        if (setBrowserTitle && title) {
            setPreviousTitle(document.title);
            document.title = `ProjectForge - ${title}`;

            return () => {
                document.title = previousTitle;
            };
        }

        return () => {
        };
    }, [setBrowserTitle, title]);

    // Render PageMenu if the option displayPageMenu is true.
    const menu = React.useMemo(() => (
        displayPageMenu && <DynamicPageMenu menu={pageMenu} title={title} />
    ), [displayPageMenu, pageMenu, title]);

    // Render ActionGroup if actions were found in the ui object.
    const actionGroup = React.useMemo(() => (
        actions && showActionButtons && <DynamicActionGroup actions={actions} />
    ), [actions, showActionButtons]);

    return (
        <DynamicLayoutContext.Provider
            /* eslint-disable-next-line react/jsx-no-constructed-context-values */
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
            {!disableLayoutRendering && renderLayout(layout)}
            {actionGroup}
            {historyBackButton
            && (
                <Button
                    onClick={history.goBack}
                >
                    <span id="back">{historyBackButton}</span>
                </Button>
            )}
        </DynamicLayoutContext.Provider>
    );
}

DynamicLayout.propTypes = {
    // UI Prop
    ui: PropTypes.shape({
        actions: PropTypes.arrayOf(buttonPropType),
        layout: PropTypes.instanceOf(Array),
        title: PropTypes.string,
        pageMenu: PropTypes.arrayOf(menuItemPropType),
        historyBackButton: PropTypes.string,
    }).isRequired,
    callAction: PropTypes.func,
    // Additional content to be displayed in the DynamicLayout context.
    children: PropTypes.node,
    data: PropTypes.shape({}),
    // Customization options
    options: PropTypes.shape({
        disableLayoutRendering: PropTypes.bool,
        displayPageMenu: PropTypes.bool,
        setBrowserTitle: PropTypes.bool,
        showActionButtons: PropTypes.bool,
        showPageMenuTitle: PropTypes.bool,
    }),
    setData: PropTypes.func,
    setVariables: PropTypes.func,
    validationErrors: PropTypes.arrayOf(PropTypes.shape({
        message: PropTypes.string,
        fieldId: PropTypes.string,
    })),
    variables: PropTypes.shape({}),
};

DynamicLayout.defaultProps = {
    callAction: dynamicLayoutContextDefaultValues.callAction,
    children: undefined,
    data: {},
    options: dynamicLayoutContextDefaultValues.options,
    setData: dynamicLayoutContextDefaultValues.setData,
    setVariables: dynamicLayoutContextDefaultValues.setVariables,
    validationErrors: [],
    variables: {},
};

export default DynamicLayout;
