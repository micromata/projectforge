import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Collapse, Nav, Navbar, NavbarToggler, } from '../../design';
import { categoryPropType } from '../../../utilities/propTypes';
import CategoriesDropdown from './categories-dropdown';

class Navigation extends Component {
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
        const { mobileIsOpen } = this.state;
        const { categories } = this.props;

        return (
            <Navbar color="light" light expand="md">
                <NavbarToggler onClick={this.toggleMobile} className="ml-auto" />
                <Collapse isOpen={mobileIsOpen} navbar>
                    <Nav className="mr-auto" navbar>
                        {
                            categories !== null && categories.length > 0
                                ? <CategoriesDropdown categories={categories} />
                                : undefined
                        }
                    </Nav>
                </Collapse>
            </Navbar>
        );
    }
}

Navigation.propTypes = {
    categories: PropTypes.arrayOf(categoryPropType),
};

Navigation.defaultProps = {
    categories: [],
};

export default Navigation;
