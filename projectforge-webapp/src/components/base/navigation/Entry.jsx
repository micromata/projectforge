import React from 'react';
import { Link } from 'react-router-dom';
import { menuItemPropType } from '../../../utilities/propTypes';
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

    if (entry.subMenu) {
        content = (
            <UncontrolledDropdown nav inNavbar>
                <DropdownToggle nav caret>
                    {entry.title}
                </DropdownToggle>
                <DropdownMenu>
                    {entry.subMenu.map(item => (
                        <DropdownItem
                            key={`entry-item-${entry.key}-${item.key}`}
                            className={style.entryItem}
                        >
                            <Link to={`/${item.url}`}>{item.title}</Link>
                        </DropdownItem>
                    ))}
                </DropdownMenu>
            </UncontrolledDropdown>
        );
    } else {
        content = (
            <NavItem>
                <NavLink tag={Link} to={`/${entry.url}`}>
                    {entry.title}
                </NavLink>
            </NavItem>
        );
    }

    return content;
}

Entry.propTypes = {
    entry: menuItemPropType.isRequired,
};

export default Entry;
