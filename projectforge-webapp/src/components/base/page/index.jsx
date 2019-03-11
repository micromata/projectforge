import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Collapse, Nav, Navbar, NavbarToggler, NavItem, } from '../../design';
import style from './Page.module.scss';

class Page extends Component {
    constructor(props) {
        super(props);

        this.state = {
            mobileIsOpen: false,
        };

        this.toggleMobile = this.toggleMobile.bind(this);
    }

    toggleMobile() {
        const { mobileIsOpen } = this.state;

        this.setState({
            mobileIsOpen: !mobileIsOpen,
        });
    }


    render() {
        const { title } = this.props;
        const { mobileIsOpen } = this.state;

        return (
            <React.Fragment>
                <Navbar expand="md" color="light" light>
                    <NavbarToggler onClick={this.toggleMobile} className="ml-auto" />
                    <Collapse isOpen={mobileIsOpen} navbar>
                        <Nav className="mr-auto" navbar>
                            <NavItem className={style.title}>
                                {title}
                            </NavItem>
                        </Nav>
                    </Collapse>
                </Navbar>
            </React.Fragment>
        );
    }
}

Page.propTypes = {
    title: PropTypes.string,
};

Page.defaultProps = {
    title: 'Options',
};

export default Page;
