import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { loadMenu, logoutUser } from '../../../actions';
import { badgePropType, categoryPropType, menuItemPropType } from '../../../utilities/propTypes';
import {
    Collapse,
    DropdownItem,
    DropdownMenu,
    DropdownToggle,
    Nav,
    Navbar,
    NavbarToggler,
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

    componentDidMount() {
        const { loadNavigation } = this.props;

        loadNavigation();
    }

    toggleMobile() {
        this.setState(state => ({
            mobileIsOpen: !state.mobileIsOpen,
        }));
    }

    render() {
        const { mobileIsOpen } = this.state;
        const {
            badge,
            categories,
            entries,
            username,
            logout,
        } = this.props;

        return (
            <Navbar color="light" light expand="md" className={style.navigation}>
                <NavbarToggler
                    onClick={this.toggleMobile}
                    className="ml-auto"
                    aria-label="toggleMobileNavbar"
                />
                <Collapse isOpen={mobileIsOpen} navbar aria-label="navbar-collapse">
                    <Nav className="mr-auto" navbar>
                        {categories !== null && categories.length > 0
                            ? <CategoriesDropdown categories={categories} badge={badge} />
                            : undefined
                        }
                        {entries !== null && entries.length > 0
                            ? entries.map(entry => (
                                <Entry key={`navigation-entry-${entry.name}`} entry={entry} />
                            ))
                            : undefined
                        }
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
    loadNavigation: PropTypes.func.isRequired,
    logout: PropTypes.func.isRequired,
    categories: PropTypes.arrayOf(menuItemPropType).isRequired,
    badge: badgePropType,
    entries: PropTypes.arrayOf(categoryPropType),
    username: PropTypes.string,
};

Navigation.defaultProps = {
    badge: undefined,
    entries: [],
    username: 'You',
};

const mapStateToProps = state => ({
    username: state.authentication.user.fullname,
    categories: state.menu.categories,
    badge: state.menu.badge,
});

const actions = {
    loadNavigation: loadMenu,
    logout: logoutUser,
};

export default connect(mapStateToProps, actions)(Navigation);
