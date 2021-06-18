import { faComment } from '@fortawesome/free-regular-svg-icons';
import { faMobileAlt, faPhone } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';
import styles from './PhoneNumber.module.scss';

function PhoneNumber(
    {
        addressId,
        number,
        phoneCallEnabled,
        phoneType,
        sms,
        smsEnabled,
    },
) {
    const stopPropagation = (event) => event.stopPropagation();

    const phoneIcon = phoneType === 'MOBILE' || phoneType === 'PRIVATE_MOBILE'
        ? faMobileAlt
        : faPhone;

    return (
        <div className={styles.number}>
            {phoneCallEnabled ? (
                <Link
                    onClick={stopPropagation}
                    to={`/wa/phoneCall?addressId=${addressId}&number=${encodeURIComponent(number)}`}
                >
                    {number}
                </Link>
            ) : number}
            {smsEnabled && sms ? (
                <Link
                    onClick={stopPropagation}
                    to={`/wa/sendSms?addressId=${addressId}&phoneType=${encodeURIComponent(phoneType)}`}
                >
                    <FontAwesomeIcon icon={faComment} className={styles.smsIcon} />
                </Link>
            ) : undefined}
            <span className={styles.zoom}>
                <FontAwesomeIcon icon={phoneIcon} className={styles.icon} />
                {number}
            </span>
        </div>
    );
}

PhoneNumber.propTypes = {
    addressId: PropTypes.number.isRequired,
    number: PropTypes.string.isRequired,
    phoneCallEnabled: PropTypes.bool.isRequired,
    phoneType: PropTypes.string.isRequired,
    smsEnabled: PropTypes.bool.isRequired,
    sms: PropTypes.bool,
};

PhoneNumber.defaultProps = {
    sms: false,
};

export default PhoneNumber;
