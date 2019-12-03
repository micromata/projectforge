import React from 'react';

import { faComment } from '@fortawesome/free-regular-svg-icons/index';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';

function CustomizedAddressPhoneNumbers({ data }) {
    const { address } = data;
    // Fragen an Fin:
    //  - Klick auf Telefonnummern (SMS) sollte dann dem Link folgen, nicht zur Editseite.
    //  - smsEnabled und phoneCallEnabled aus den Variablen des Servers bekommen.
    //  - Sollen für die Adressbücher in der Liste auch Customized-Elemente gebaut werden?
    // const { variables } = React.useContext(DynamicLayoutContext);

    const phoneNumbers = [];

    // console.log(variables.smsEnabled);
    // const { smsEnabled, phoneCallEnabled } = variables;
    const smsEnabled = true;
    const phoneCallEnabled = true;

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
                return <React.Fragment />;
            }
            add(address.businessPhone, 'BUSINESS', false, 0);
            add(address.mobilePhone, 'MOBILE', true, 1);
            add(address.privatePhone, 'PRIVATE', false, 2);
            add(address.privateMobilePhone, 'PRIVATE_MOBILE', true, 3);

            return (
                <React.Fragment>
                    {phoneNumbers.map((value, index) => {
                        const lineBreak = index > 0 ? <br /> : undefined;
                        const phoneNumber = phoneCallEnabled ? (
                            <Link
                                to={`/wa/phoneCall?addressId=${address.id}&number=${encodeURIComponent(value.number)}`}
                            >
                                {value.number}
                            </Link>
                        ) : <React.Fragment>{value.number}</React.Fragment>;
                        const sms = smsEnabled && value.sms ? (
                            <Link
                                to={`/wa/sendSms?addressId=${address.id}&phoneType=${value.phoneType}`}
                            >
                                {' '}
                                <FontAwesomeIcon icon={faComment} />
                            </Link>
                        ) : undefined;
                        return (
                            <React.Fragment key={value.key}>
                                {lineBreak}
                                {phoneNumber}
                                {sms}
                            </React.Fragment>
                        );
                    })}
                </React.Fragment>
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
