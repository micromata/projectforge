import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import ReactMarkdown from 'react-markdown';
import { colorPropType } from '../../../../utilities/propTypes';
import { Alert } from '../../../design';

function DynamicAlert(props) {
    const {
        message,
        markdown,
        title,
        color,
        icon,
    } = props;

    let box = message;
    if (markdown === true) {
        box = <ReactMarkdown>{message}</ReactMarkdown>;
    }

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
            {box}
        </Alert>
    );
}

DynamicAlert.propTypes = {
    message: PropTypes.string.isRequired,
    markdown: PropTypes.bool,
    title: PropTypes.string,
    color: colorPropType,
    icon: PropTypes.arrayOf(PropTypes.string),
};

DynamicAlert.defaultProps = {
    markdown: undefined,
    title: undefined,
    color: undefined,
    icon: undefined,
};

export default DynamicAlert;
