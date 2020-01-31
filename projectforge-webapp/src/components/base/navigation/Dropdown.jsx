import { faChevronDown, faCog } from '@fortawesome/free-solid-svg-icons/index';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import PropTypes from 'prop-types';
import React from 'react';
import { menuItemPropType } from '../../../utilities/propTypes';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown, } from '../../design';
import NavigationAction from './Action';
import MenuBadge from './categories-dropdown/MenuBadge';

function NavigationDropdown(
    {
        badge,
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
                {badge && badge.counter && entryKey && (
                    <MenuBadge elementKey={entryKey} isFlying color={badge.style || 'danger'}>
                        {badge.counter}
                    </MenuBadge>
                )}
                <FontAwesomeIcon icon={faChevronDown} />
            </DropdownToggle>
            <DropdownMenu>
                {subMenu.map(item => (
                    <DropdownItem
                        key={`entry-item-${entryKey || id}-${item.key || item.id}`}
                    >
                        <NavigationAction
                            badgeIsFlying={false}
                            entryKey={item.key}
                            {...item}
                        />
                    </DropdownItem>
                ))}
            </DropdownMenu>
        </UncontrolledDropdown>
    );
}

NavigationDropdown.propTypes = {
    title: PropTypes.string.isRequired,
    subMenu: PropTypes.arrayOf(menuItemPropType).isRequired,
    badge: PropTypes.shape({
        counter: PropTypes.number,
    }),
    entryKey: PropTypes.string,
    id: PropTypes.string,
};

NavigationDropdown.defaultProps = {
    badge: undefined,
    id: undefined,
    entryKey: undefined,
};

export default NavigationDropdown;
