import PropTypes from 'prop-types';
import React from 'react';
import { Label } from '../../../design';

function DynamicLabel({ label }) {
    return <Label>{label}</Label>;
}

DynamicLabel.propTypes = {
    label: PropTypes.string.isRequired,
};

DynamicLabel.defaultProps = {};

export default DynamicLabel;
