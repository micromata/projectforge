import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { Alert } from '../../../design';

function DynamicAlert(props) {
    const {
        message,
        title,
        color,
        icon,
    } = props;

    return (
        <Alert color={color}>
            {title && (
                <h4 className="alert-heading">
                    {title}
                </h4>
            )}
            {icon && (
                <>
                    <FontAwesomeIcon icon={icon} />
                    &nbsp;&nbsp;
                </>
            )}
            {message}
        </Alert>
    );
}

DynamicAlert.propTypes = {
    message: PropTypes.string.isRequired,
    title: PropTypes.string,
    color: colorPropType,
    icon: PropTypes.arrayOf(PropTypes.string),
};

DynamicAlert.defaultProps = {
    title: undefined,
    color: undefined,
    icon: undefined,
};

export default DynamicAlert;
