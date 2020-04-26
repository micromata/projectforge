import React from 'react';
import PropTypes from 'prop-types';

function CustomizedAddressView({ values }) {
    return React.useMemo(
        () => (
            <React.Fragment>
                {values.address
                    ? (
                        <React.Fragment>
                            {values.address}
                            <br />
                        </React.Fragment>
                    ) : undefined}
                {values.zipcode || values.city
                    ? (
                        <React.Fragment>
                            {`${values.zipCode} ${values.city}`}
                            <br />
                        </React.Fragment>
                    ) : undefined}
                {values.state
                    ? (
                        <React.Fragment>
                            {values.state}
                            <br />
                        </React.Fragment>
                    ) : undefined}
                {values.country
                    ? (
                        <React.Fragment>
                            {values.country}
                            <br />
                        </React.Fragment>
                    ) : undefined}
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
