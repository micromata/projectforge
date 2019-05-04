import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { Navbar } from '../../design';
import Navigation from '../navigation';

function DynamicPageMenu({ menu, title }) {
    // Return fragment when the menu is undefined
    if (menu === undefined) {
        return <React.Fragment />;
    }

    // Add the title as the first entry.
    if (title) {
        menu.unshift({
            title,
            type: 'TEXT',
            key: 'dynamic-page-menu-title',
        });
    }

    // Build a navigation with the menu(-entries) from the props.
    return (
        <Navbar>
            <Navigation
                entries={menu}
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
