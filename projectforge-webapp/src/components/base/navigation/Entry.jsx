import React from 'react';
import { NavItem } from '../../design';
import NavigationAction from './Action';

function NavigationEntry(props) {
    return (
        <NavItem>
            <NavigationAction {...props} />
        </NavItem>
    );
}

export default NavigationEntry;
