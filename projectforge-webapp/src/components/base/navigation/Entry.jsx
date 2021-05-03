import React from 'react';
import { NavItem } from '../../design';
import NavigationAction from './Action';

/* eslint-disable react/jsx-props-no-spreading */

function NavigationEntry(props) {
    return (
        <NavItem>
            <NavigationAction {...props} />
        </NavItem>
    );
}

export default NavigationEntry;
