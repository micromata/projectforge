import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { Nav } from '../../design';
import NavigationDropdown from './Dropdown';
import NavigationEntry from './Entry';

function Navigation({ entries, className = 'ml-auto', right = false }) {
    return (
        <Nav className={className}>
            {entries.map((entry) => {
                let Tag;

                if (entry.subMenu) {
                    Tag = NavigationDropdown;
                } else {
                    Tag = NavigationEntry;
                }
                return (
                    <Tag
                        {...entry}
                        key={entry.key || entry.id}
                        entryKey={entry.key}
                        right={right}
                    />
                );
            })}
        </Nav>
    );
}

Navigation.propTypes = {
    entries: PropTypes.arrayOf(menuItemPropType).isRequired,
    className: PropTypes.string,
    right: PropTypes.bool,
};

export default Navigation;
