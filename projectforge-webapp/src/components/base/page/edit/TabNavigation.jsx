import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import { Nav, NavItem, NavLink } from '../../../design';
import style from '../Page.module.scss';

function TabNavigation({ tabs, activeTab, ...props }) {
    return (
        <Nav tabs {...props}>
            {tabs.map(tab => (
                <NavItem key={tab.id}>
                    <NavLink
                        id={tab.id}
                        className={classNames(
                            style.navTab,
                            { [style.active]: activeTab === tab.id },
                        )}
                        tag={Link}
                        to={tab.link}
                    >
                        {tab.title}
                    </NavLink>
                </NavItem>
            ))}
        </Nav>
    );
}

TabNavigation.propTypes = {
    tabs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        title: PropTypes.string,
        link: PropTypes.string,
    })),
    activeTab: PropTypes.string,
};

TabNavigation.defaultProps = {
    tabs: [],
    activeTab: undefined,
};

export default TabNavigation;
