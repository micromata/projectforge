import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { Nav, NavItem, NavLink, } from '../../../design';
import style from '../Page.module.scss';

function TabNavigation({ tabs, toggleTab, activeTab }) {
    return (
        <Nav tabs>
            {Object.keys(tabs)
                .map(id => (
                    <NavItem key={id}>
                        <NavLink
                            id={id}
                            className={
                                classNames(style.navTab, { [style.active]: activeTab === id })
                            }
                            onClick={toggleTab}
                        >
                            {tabs[id]}
                        </NavLink>
                    </NavItem>
                ))}
        </Nav>
    );
}

TabNavigation.propTypes = {
    tabs: PropTypes.objectOf(PropTypes.string),
    /*
    tabs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string,
        title: PropTypes.string,
    })),
    */
    toggleTab: PropTypes.func,
    activeTab: PropTypes.string,
};

TabNavigation.defaultProps = {
    tabs: [],
    toggleTab: undefined,
    activeTab: undefined,
};

export default TabNavigation;
