import PropTypes from 'prop-types';
import React from 'react';
import {
    Collapse,
    Nav,
    Navbar,
    NavbarToggler,
    NavItem,
} from '../../design';
import style from './Page.module.scss';

class PageNavigation extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            mobileIsOpen: false,
        };

        this.toggleMobile = this.toggleMobile.bind(this);
    }

    toggleMobile() {
        this.setState(state => ({
            mobileIsOpen: !state.mobileIsOpen,
        }));
    }

    render() {
        const { mobileIsOpen } = this.state;
        const { current } = this.props;

        return (
            <Navbar expand="md" color="light" light className={style.navigation}>
                <NavbarToggler onClick={this.toggleMobile} className="ml-auto" />
                <Collapse isOpen={mobileIsOpen} navbar>
                    <Nav className="mr-auto" navbar>
                        <NavItem className={style.title}>
                            {current}
                        </NavItem>
                    </Nav>
                </Collapse>
            </Navbar>
        );
    }
}

PageNavigation.propTypes = {
    current: PropTypes.string,
};

PageNavigation.defaultProps = {
    current: 'Loading',
};

export default PageNavigation;
