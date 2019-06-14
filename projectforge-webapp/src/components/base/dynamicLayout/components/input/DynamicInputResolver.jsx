import PropTypes from 'prop-types';
import React from 'react';
import DynamicInput from './DynamicInput';

// All types of 'INPUT' will be resolved here.
function DynamicInputResolver({ dataType, ...props }) {
    let Tag;

    switch (dataType) {
        case 'STRING':
            Tag = DynamicInput;
            break;
        default:
            return <span>{`${dataType} Input is not implemented.`}</span>;
    }

    return (
        <Tag {...props} />
    );
}

DynamicInputResolver.propTypes = {
    dataType: PropTypes.oneOf([
        // All dataTypes yet implemented for type 'INPUT'.
        'STRING',
    ]).isRequired,
};

DynamicInputResolver.defaultProps = {};

export default DynamicInputResolver;
