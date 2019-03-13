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
        const { mobileIsOpen } = this.state;

        this.setState({
            mobileIsOpen: !mobileIsOpen,
        });
    }

    render() {
        const { mobileIsOpen } = this.state;

        return (
            <Navbar expand="md" color="light" light className={style.navigation}>
                <NavbarToggler onClick={this.toggleMobile} className="ml-auto" />
                <Collapse isOpen={mobileIsOpen} navbar>
                    <Nav className="mr-auto" navbar>
                        <NavItem className={style.title}>
                            TODO: INSERT TITLE TREE AND ADDITIONAL COMPONENTS
                        </NavItem>
                    </Nav>
                </Collapse>
            </Navbar>
        );
    }
}

export default PageNavigation;
