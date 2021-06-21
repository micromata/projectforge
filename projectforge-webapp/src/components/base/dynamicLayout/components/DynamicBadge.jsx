import PropTypes from 'prop-types';
import React from 'react';
import { Badge } from 'reactstrap';
import { colorPropType } from '../../../../utilities/propTypes';

function DynamicBadge(props) {
    const {
        title,
        color,
    } = props;

    return (
        <h4>
            <Badge color={color} pill>{title}</Badge>
        </h4>
    );
}

DynamicBadge.propTypes = {
    title: PropTypes.string.isRequired,
    color: colorPropType,
};

DynamicBadge.defaultProps = {
    color: undefined,
};

export default DynamicBadge;
