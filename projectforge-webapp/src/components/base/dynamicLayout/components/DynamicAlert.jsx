import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { Alert } from '../../../design';

function DynamicAlert(props) {
    const { message, color, icon } = props;

    return (
        <Alert color={color}>
            {icon && (
                <React.Fragment>
                    <FontAwesomeIcon icon={icon} />
                    &nbsp;&nbsp;
                </React.Fragment>
            )}
            {message}
        </Alert>
    );
}

DynamicAlert.propTypes = {
    message: PropTypes.string.isRequired,
    color: colorPropType,
    icon: PropTypes.arrayOf(PropTypes.string),
};

DynamicAlert.defaultProps = {
    color: undefined,
    icon: undefined,
};

export default DynamicAlert;
