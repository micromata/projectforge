import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { categoryItemPropType, categoryPropType } from '../../../utilities/propTypes';
import {
    DropdownItem,
    DropdownMenu,
    DropdownToggle,
    NavItem,
    NavLink,
    UncontrolledDropdown,
} from '../../design';
import style from './Navigation.module.scss';

function Entry({ entry }) {
    let content;

    if (entry.items) {
        content = (
            <UncontrolledDropdown>
                <DropdownToggle nav caret>
                    {entry.name}
                </DropdownToggle>
                <DropdownMenu>
                    {entry.items.map(item => (
                        <DropdownItem
                            key={`entry-item-${entry.name}-${item.name}`}
                            className={style.entryItem}
                        >
                            <Link to={item.url}>{item.name}</Link>
                        </DropdownItem>
                    ))}
                </DropdownMenu>
            </UncontrolledDropdown>
        );
    } else {
        content = (
            <NavItem>
                <NavLink tag={Link} to={entry.url}>
                    {entry.name}
                </NavLink>
            </NavItem>
        );
    }

    return content;
}

Entry.propTypes = {
    entry: PropTypes.oneOfType([categoryItemPropType, categoryPropType]).isRequired,
};

export default Entry;
