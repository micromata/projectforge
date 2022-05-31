import React from 'react';
import PropTypes from 'prop-types';
import PhoneNumber from '../../../../../design/phoneNumber/PhoneNumber';

function CustomizedAddressPhoneNumber({ values }) {
    const { data } = values;

    return React.useMemo(
        () => (
            <PhoneNumber
                addressId={data.addressId}
                number={data.number}
                phoneCallEnabled={data.phoneCallEnabled}
                phoneType={data.phoneType}
                sms={data.sms}
                smsEnabled={data.smsEnabled}
            />
        ),
        [data],
    );
}

CustomizedAddressPhoneNumber.propTypes = {
    values: PropTypes.shape({}).isRequired,
};

CustomizedAddressPhoneNumber.defaultProps = {};

export default CustomizedAddressPhoneNumber;
