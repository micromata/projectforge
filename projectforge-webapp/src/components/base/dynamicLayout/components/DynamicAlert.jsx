import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { Alert } from '../../../design';

function DynamicAlert(props) {
    console.log(props);
    const { message, color } = props;

    return (
        <Alert color={color}>
            {message}
        </Alert>
    );
}

DynamicAlert.propTypes = {
    message: PropTypes.string.isRequired,
    color: colorPropType,
};

DynamicAlert.defaultProps = {
    color: undefined,
};

export default DynamicAlert;
