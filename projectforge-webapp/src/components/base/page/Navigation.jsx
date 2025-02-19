import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Collapse, Nav, Navbar, NavbarToggler, NavItem } from '../../design';
import style from './Page.module.scss';

function PageNavigation({ current = 'Loading', children }) {
    const [mobileIsOpen, setMobileIsOpen] = useState(false);

    const toggleMobile = () => {
        setMobileIsOpen((prevState) => !prevState);
    };

    return (
        <Navbar expand="md" color="light" light className={style.navigation}>
            <NavbarToggler onClick={toggleMobile} className="ml-auto" />
            <Collapse isOpen={mobileIsOpen} navbar>
                <Nav className="mr-auto" navbar>
                    <NavItem className={style.title}>
                        {current}
                    </NavItem>
                    {children}
                </Nav>
            </Collapse>
        </Navbar>
    );
}

PageNavigation.propTypes = {
    current: PropTypes.string,
    children: PropTypes.node,
};

export default PageNavigation;
