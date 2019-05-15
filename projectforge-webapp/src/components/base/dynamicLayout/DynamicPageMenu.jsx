import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { Navbar } from '../../design';
import Navigation from '../navigation';
import { DynamicLayoutContext } from './context';

function DynamicPageMenu({ menu, title }) {
    // Return fragment when the menu is undefined
    if (menu === undefined || menu.length === 0) {
        return <React.Fragment />;
    }

    // Work with copy of menu
    const navigationMenu = [...menu];

    // Load options from context
    const { options } = React.useContext(DynamicLayoutContext);

    // Add the title as the first entry if present and option showPageMenuTitle is true.
    if (options.showPageMenuTitle && title) {
        navigationMenu.unshift({
            title,
            type: 'TEXT',
            key: 'dynamic-page-menu-title',
        });
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
