import PropTypes from 'prop-types';
import React from 'react';

function DynamicSpacer(props) {
    const { width } = props;

    return (
        <div style={{ width: `${width}em` }}>&nbsp;</div>
    );
}

DynamicSpacer.propTypes = {
    width: PropTypes.number,
};

DynamicSpacer.defaultProps = {
    width: 1,
};

export default DynamicSpacer;
