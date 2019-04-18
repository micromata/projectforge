import { faChevronDown, faCog } from '@fortawesome/free-solid-svg-icons/index';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown, } from '../../design';
import NavigationAction from './Action';

function NavigationDropdown(
    {
        entryKey,
        title,
        subMenu,
        id,
    },
) {
    let displayTitle = title;

    if (id === 'GEAR') {
        displayTitle = <FontAwesomeIcon icon={faCog} />;
    }

    return (
        <UncontrolledDropdown nav>
            <DropdownToggle nav>
                {displayTitle}
                <FontAwesomeIcon icon={faChevronDown} />
            </DropdownToggle>
            <DropdownMenu>
                {subMenu.map(item => (
                    <DropdownItem
                        key={`entry-item-${entryKey || id}-${item.key || item.id}`}
                    >
                        <NavigationAction {...item} />
                    </DropdownItem>
                ))}
            </DropdownMenu>
        </UncontrolledDropdown>
    );
}

NavigationDropdown.propTypes = {
    title: PropTypes.string.isRequired,
    subMenu: PropTypes.arrayOf(menuItemPropType).isRequired,
    entryKey: PropTypes.string,
    id: PropTypes.string,
};

NavigationDropdown.defaultProps = {
    id: undefined,
    entryKey: undefined,
};

export default NavigationDropdown;
