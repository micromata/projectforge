import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { Navbar } from '../../design';
import Navigation from '../navigation';
import { DynamicLayoutContext } from './context';

function DynamicPageMenu({ menu, title }) {
    // Load options from context
    const { options } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        // Work with clone of menu
        const navigationMenu = [...menu || []];

        // Add the title as the first entry if present and option showPageMenuTitle is true.
        if (options.showPageMenuTitle && title) {
            navigationMenu.unshift({
                title,
                type: 'TEXT',
                key: 'dynamic-page-menu-title',
            });
        }

        // Return fragment when the menu is empty
        if (navigationMenu === undefined || navigationMenu.length === 0) {
            return <></>;
        }

        // Build a navigation with the menu(-entries) from the props.
        return (
            <Navbar>
                <Navigation
                    entries={navigationMenu}
                    // Let the menu float to the right.
                    className="ml-auto"
                />
            </Navbar>
        );
    }, [menu, title, options]);
}

DynamicPageMenu.propTypes = {
    menu: PropTypes.arrayOf(menuItemPropType),
    title: PropTypes.string,
};

DynamicPageMenu.defaultProps = {
    menu: undefined,
    title: undefined,
};

export default DynamicPageMenu;
