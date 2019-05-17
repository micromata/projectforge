import PropTypes from 'prop-types';
import React from 'react';
import DynamicInput from './DynamicInput';

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
        'STRING',
    ]).isRequired,
};

DynamicInputResolver.defaultProps = {};

export default DynamicInputResolver;
