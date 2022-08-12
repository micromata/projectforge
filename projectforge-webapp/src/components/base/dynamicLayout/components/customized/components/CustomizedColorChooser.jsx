import React from 'react';
import PropTypes from 'prop-types';

function CustomizedColorChooser({ values }) {
    const { label, value } = values;
    return React.useMemo(
        () => (
            <div>
                {label}
                :
                {value}
            </div>
        ),
        [
            values.label,
            values.value,
        ],
    );
}

CustomizedColorChooser.propTypes = {
    values: PropTypes.shape({
        label: PropTypes.string.isRequired,
        value: PropTypes.string.isRequired,
    }).isRequired,
};

CustomizedColorChooser.defaultProps = {};

export default CustomizedColorChooser;
