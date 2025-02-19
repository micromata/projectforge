import PropTypes from 'prop-types';
import React from 'react';
import { Badge } from 'reactstrap';
import { colorPropType } from '../../../../utilities/propTypes';

function DynamicBadge(props) {
    const {
        title,
        color,
        pill = false,
    } = props;

    return (
        <Badge color={color} pill={pill}>{title}</Badge>
    );
}

DynamicBadge.propTypes = {
    title: PropTypes.string.isRequired,
    color: colorPropType,
    pill: PropTypes.bool,
};

export default DynamicBadge;
