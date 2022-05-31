import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { loadMenu } from '../../../actions';
import { badgePropType, menuItemPropType } from '../../../utilities/propTypes';
import { Collapse, Navbar, NavbarToggler } from '../../design';
import CategoriesDropdown from './categories-dropdown';
import Navigation from './index';
import style from './Navigation.module.scss';

class GlobalNavigation extends React.Component {
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
        this.setState((state) => ({ mobileIsOpen: !state.mobileIsOpen }));
    }

    render() {
        const { mobileIsOpen } = this.state;
        const {
            badge,
            favoritesMenu,
            mainMenu,
            myAccountMenu,
        } = this.props;

        return (
            <Navbar color="light" light expand="md" className={style.globalNavigation}>
                <NavbarToggler
                    onClick={this.toggleMobile}
                    className="ml-auto"
                />
                <Collapse isOpen={mobileIsOpen} navbar>
                    {mainMenu && mainMenu.length
                        ? <CategoriesDropdown categories={mainMenu} badge={badge} />
                        : undefined}
                    {favoritesMenu && favoritesMenu.length > 0
                        ? <Navigation entries={favoritesMenu} className="mr-auto" />
                        : undefined}
                    {myAccountMenu && myAccountMenu.length > 0
                        ? <Navigation entries={myAccountMenu} className="ml-auto" />
                        : undefined}
                </Collapse>
            </Navbar>
        );
    }
}

GlobalNavigation.propTypes = {
    favoritesMenu: PropTypes.arrayOf(menuItemPropType).isRequired,
    loadNavigation: PropTypes.func.isRequired,
    mainMenu: PropTypes.arrayOf(menuItemPropType).isRequired,
    myAccountMenu: PropTypes.arrayOf(menuItemPropType).isRequired,
    badge: badgePropType,
};

GlobalNavigation.defaultProps = {
    badge: undefined,
};

const mapStateToProps = (state) => ({ ...state.menu });

const actions = {
    loadNavigation: loadMenu,
};

export default connect(mapStateToProps, actions)(GlobalNavigation);
