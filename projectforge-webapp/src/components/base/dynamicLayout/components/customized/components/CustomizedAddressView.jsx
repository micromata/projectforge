import React from 'react';
import PropTypes from 'prop-types';

function CustomizedAddressView({ values }) {
    function add(line) {
        if (line && line.length > 0) {
            return (
                <>
                    {line}
                    <br />
                </>
            );
        }
        return undefined;
    }

    const city = (values.zipcode || values.city) ? `${values.zipCode} ${values.city}` : undefined;

    return React.useMemo(
        () => (
            <>
                {add(values.address)}
                {add(values.address2)}
                {add(city)}
                {add(values.state)}
                {add(values.country)}
            </>
        ),
        [
            values,
        ],
    );
}

CustomizedAddressView.propTypes = {
    values: PropTypes.shape({
        address: PropTypes.string,
    }).isRequired,
};

CustomizedAddressView.defaultProps = {};

export default CustomizedAddressView;
