import PropTypes from 'prop-types';
import React from 'react';

function DynamicSpacer({ width = 1 }) {
    return (
        <div style={{ width: `${width}em` }}>&nbsp;</div>
    );
}

DynamicSpacer.propTypes = {
    width: PropTypes.number,
};

export default DynamicSpacer;
