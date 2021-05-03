import PropTypes from 'prop-types';
import React from 'react';
import PhoneNumber from '../../../../../design/phoneNumber/PhoneNumber';
import { DynamicLayoutContext } from '../../../context';

function CustomizedAddressPhoneNumbers({ data }) {
    const { address } = data;
    const { variables } = React.useContext(DynamicLayoutContext);

    const phoneNumbers = [];

    const { smsEnabled, phoneCallEnabled } = variables;

    function add(number, phoneType, sms, key) {
        if (number && number.trim().length > 0) {
            phoneNumbers.push({
                number,
                phoneType,
                sms,
                key,
            });
        }
    }

    return React.useMemo(
        () => {
            if (!address) {
                return <></>;
            }

            add(address.businessPhone, 'BUSINESS', false, 0);
            add(address.mobilePhone, 'MOBILE', true, 1);
            add(address.privatePhone, 'PRIVATE', false, 2);
            add(address.privateMobilePhone, 'PRIVATE_MOBILE', true, 3);

            return (
                <>
                    {phoneNumbers.map((value) => (
                        <PhoneNumber
                            addressId={address.id}
                            phoneCallEnabled={phoneCallEnabled}
                            smsEnabled={smsEnabled}
                            {...value}
                        />
                    ))}
                </>
            );
        },
        [address],
    );
}

CustomizedAddressPhoneNumbers.propTypes = {
    data: PropTypes.shape({
        address: PropTypes.shape({
            businessPhone: PropTypes.string,
            mobilePhone: PropTypes.string,
            privatePhone: PropTypes.string,
            privateMobilePhone: PropTypes.string,
        }),
    }).isRequired,

};

CustomizedAddressPhoneNumbers.defaultProps = {};

export default CustomizedAddressPhoneNumbers;
