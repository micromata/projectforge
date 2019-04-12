import PropTypes from 'prop-types';
import React from 'react';

function DropdownSelectContent() {
    return <h1>Select</h1>;
}

DropdownSelectContent.propTypes = {
    value: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number,
        title: PropTypes.string,
    })),
};

DropdownSelectContent.defaultProps = {
    value: '',
    values: [],
};


export default DropdownSelectContent;
