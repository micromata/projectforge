import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { useNavigate, useLocation } from 'react-router';
import { Nav, NavItem, NavLink } from '../../../design';
import style from '../Page.module.scss';

function TabNavigation({ tabs = [], activeTab, ...props }) {
    const navigate = useNavigate();
    const location = useLocation();

    const handleTabClick = (e, tabLink) => {
        e.preventDefault();
        // Preserve location state (including modal background) when navigating between tabs
        navigate(tabLink, { state: location.state });
    };

    return (
        <Nav tabs {...props}>
            {tabs.map((tab) => (
                <NavItem key={tab.id}>
                    <NavLink
                        id={tab.id}
                        className={classNames(
                            style.navTab,
                            { [style.active]: activeTab === tab.id },
                        )}
                        href={tab.link}
                        onClick={(e) => handleTabClick(e, tab.link)}
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

export default TabNavigation;
