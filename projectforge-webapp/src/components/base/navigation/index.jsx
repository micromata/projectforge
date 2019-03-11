import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Collapse, Nav, Navbar, NavbarToggler, } from '../../design';
import { categoryPropType } from '../../../utilities/propTypes';
import CategoriesDropdown from './categories-dropdown';
import Entry from './Entry';
import style from './Navigation.module.scss';

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
        const { categories, entries } = this.props;

        return (
            <Navbar color="light" light expand="md" className={style.navigation}>
                <NavbarToggler onClick={this.toggleMobile} className="ml-auto" />
                <Collapse isOpen={mobileIsOpen} navbar>
                    <Nav className="mr-auto" navbar>
                        {categories !== null && categories.length > 0
                            ? <CategoriesDropdown categories={categories} />
                            : undefined
                        }
                        {entries !== null
                            ? entries.map(entry => (
                                <Entry key={`navigation-entry-${entry.name}`} entry={entry} />
                            ))
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
    entries: PropTypes.arrayOf(categoryPropType),
};

Navigation.defaultProps = {
    categories: [],
    entries: [],
};

export default Navigation;
