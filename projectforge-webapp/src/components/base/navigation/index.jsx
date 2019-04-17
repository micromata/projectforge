import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { Nav } from '../../design';
import NavigationDropdown from './Dropdown';
import NavigationEntry from './Entry';

function Navigation({ entries, className }) {
    return (
        <Nav className={className}>
            {entries.map((entry) => {
                let Tag;

                if (entry.subMenu) {
                    Tag = NavigationDropdown;
                } else {
                    Tag = NavigationEntry;
                }

                return <Tag key={entry.key || entry.id} {...entry} entryKey={entry.key} />;
            })}
        </Nav>
    );
}

Navigation.propTypes = {
    entries: PropTypes.arrayOf(menuItemPropType).isRequired,
    className: PropTypes.string,
};

Navigation.defaultProps = {
    className: 'ml-auto',
};

export default Navigation;
