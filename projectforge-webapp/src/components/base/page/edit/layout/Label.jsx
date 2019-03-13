import React from 'react';
import PropTypes from 'prop-types';
import { Label } from '../../../../design';

function LayoutLabel({ value, for: forComponent }) {
    return <Label for={forComponent} sm={2}>{value}</Label>;
}

LayoutLabel.propTypes = {
    value: PropTypes.string.isRequired,
    for: PropTypes.string,
};

LayoutLabel.defaultProps = {
    for: undefined,
};

export default LayoutLabel;
