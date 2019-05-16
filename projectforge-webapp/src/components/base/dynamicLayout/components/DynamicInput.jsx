import PropTypes from 'prop-types';
import React from 'react';

function DynamicInput({ dataType }) {
    return (
        <h1>Input here</h1>
    );
}

DynamicInput.propTypes = {
    dataType: PropTypes.oneOf([
        'STRING',
    ]).isRequired,
};

export default DynamicInput;
