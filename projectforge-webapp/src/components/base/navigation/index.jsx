import { faSearch } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { categoryPropType } from '../../../utilities/propTypes';
import {
    Button,
    Collapse,
    DropdownItem,
    DropdownMenu,
    DropdownToggle,
    Input,
    InputGroup,
    InputGroupAddon,
    Nav,
    Navbar,
    NavbarToggler,
    NavItem,
    UncontrolledDropdown,
} from '../../design';
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
        this.setState(state => ({
            mobileIsOpen: !state.mobileIsOpen,
        }));
    }

    render() {
        const { mobileIsOpen } = this.state;
        const {
            categories,
            entries,
            username,
            logout,
        } = this.props;

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
                        <NavItem>
                            <InputGroup>
                                <InputGroupAddon addonType="append">
                                    <Input type="text" placeholder="[Suche]" />
                                    <Button color="success">
                                        <FontAwesomeIcon icon={faSearch} />
                                    </Button>
                                </InputGroupAddon>
                            </InputGroup>
                        </NavItem>
                    </Nav>
                    <Nav className="ml-auto" navbar>
                        <UncontrolledDropdown nav inNavbar>
                            <DropdownToggle nav caret>
                                {username}
                            </DropdownToggle>
                            <DropdownMenu right>
                                <DropdownItem className={style.entryItem}>
                                    <Link to="/">
                                        [Feedback senden]
                                    </Link>
                                </DropdownItem>
                                <DropdownItem divider />
                                <DropdownItem className={style.entryItem}>
                                    <Link to="/">
                                        [Seite als Link]
                                    </Link>
                                </DropdownItem>
                                <DropdownItem className={style.entryItem}>
                                    <Link to="/">
                                        [Dokumentation]
                                    </Link>
                                </DropdownItem>
                                <DropdownItem className={style.entryItem} onClick={logout}>
                                    [Abmelden]
                                </DropdownItem>
                            </DropdownMenu>
                        </UncontrolledDropdown>
                    </Nav>
                </Collapse>
            </Navbar>
        );
    }
}

Navigation.propTypes = {
    logout: PropTypes.func,
    categories: PropTypes.arrayOf(categoryPropType),
    entries: PropTypes.arrayOf(categoryPropType),
    username: PropTypes.string,
};

Navigation.defaultProps = {
    logout: undefined,
    categories: [],
    entries: [],
    username: 'You',
};

export default Navigation;
