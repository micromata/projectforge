import PropTypes from 'prop-types';
import React from 'react';
import { Badge } from '../../index';
import BasePart from './Part';

function handleClick(event) {
    event.preventDefault();
    event.stopPropagation();
}

function BadgePart({ children }) {
    return (
        <BasePart>
            <Badge onClick={handleClick}>
                {children}
            </Badge>
        </BasePart>
    );
}

BadgePart.propTypes = {
    children: PropTypes.node.isRequired,
};

export default BadgePart;
