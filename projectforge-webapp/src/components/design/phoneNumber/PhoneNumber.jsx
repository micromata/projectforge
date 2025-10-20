import { faComment } from '@fortawesome/free-regular-svg-icons';
import { faMobileAlt, faPhone } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React, { useRef, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link } from 'react-router';
import styles from './PhoneNumber.module.scss';

function PhoneNumber(
    {
        addressId,
        number,
        phoneCallEnabled,
        phoneType,
        sms = false,
        smsEnabled,
        callerPage,
    },
) {
    const numberRef = useRef(null);
    const [isHovered, setIsHovered] = useState(false);
    const stopPropagation = (event) => event.stopPropagation();

    const phoneIcon = phoneType === 'MOBILE' || phoneType === 'PRIVATE_MOBILE'
        ? faMobileAlt
        : faPhone;

    useEffect(() => {
        const element = numberRef.current;
        if (!element) return undefined;

        const handleMouseEnter = () => setIsHovered(true);
        const handleMouseLeave = () => setIsHovered(false);

        element.addEventListener('mouseenter', handleMouseEnter);
        element.addEventListener('mouseleave', handleMouseLeave);

        return () => {
            element.removeEventListener('mouseenter', handleMouseEnter);
            element.removeEventListener('mouseleave', handleMouseLeave);
        };
    }, []);

    return (
        <div
            ref={numberRef}
            className={styles.number}
        >
            {phoneCallEnabled ? (
                <Link
                    onClick={stopPropagation}
                    to={`/wa/phoneCall?addressId=${addressId}&number=${encodeURIComponent(number)}&callerPage=${callerPage}`}
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
            {isHovered && createPortal(
                <div className={styles.zoom}>
                    <FontAwesomeIcon icon={phoneIcon} className={styles.icon} />
                    {number}
                </div>,
                document.body,
            )}
        </div>
    );
}

PhoneNumber.propTypes = {
    addressId: PropTypes.number.isRequired,
    number: PropTypes.string.isRequired,
    phoneCallEnabled: PropTypes.bool.isRequired,
    phoneType: PropTypes.string.isRequired,
    smsEnabled: PropTypes.bool.isRequired,
    callerPage: PropTypes.string.isRequired,
    sms: PropTypes.bool,
};

export default PhoneNumber;
