import React from 'react';
import PropTypes from 'prop-types';

function CustomizedAddressView({ values }) {
    function add(line) {
        if (line && line.length > 0) {
            return (
                <React.Fragment>
                    {line}
                    <br />
                </React.Fragment>
            );
        }
        return undefined;
    }

    const city = (values.zipcode || values.city) ? `${values.zipCode} ${values.city}` : undefined;

    return React.useMemo(
        () => (
            <React.Fragment>
                {add(values.address)}
                {add(values.address2)}
                {add(city)}
                {add(values.state)}
                {add(values.country)}
            </React.Fragment>
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
