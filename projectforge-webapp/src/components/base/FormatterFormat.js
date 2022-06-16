import { faCheck, faStar } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';

const formatterFormat = (
    value,
    dataType,
    dateFormat,
    timestampFormatSeconds,
    timestampFormatMinutes,
    locale,
    currency,
) => {
    let result = value;
    switch (dataType) {
        case 'COST1':
            return value.formattedNumber;
        case 'COST2':
            return value.longDisplayName || value.formattedNumber;
        case 'CURRENCY':
            return Intl.NumberFormat(locale, {
                style: 'currency',
                currency,
            }).format(value);
        case 'SHOW_DISPLAYNAME':
        case 'CUSTOMER_FORMATTER':
        case 'KONTO_FORMATTER':
        case 'PROJECT_FORMATTER':
        case 'EMPLOYEE_FORMATTER':
            return value.displayName;
        case 'SHOW_LIST_OF_DISPLAYNAMES':
            if (value && Array.isArray(value)) {
                return value.map((obj) => obj.displayName).join(', ');
            }
            return '???';
        case 'BOOLEAN':
            if (value) {
                return true;
            }
            return false;
        case 'DATE':
            return moment(value).format(dateFormat);
        case 'TASK':
            return value.title;
        case 'TIMESTAMP':
            return moment(value).format(timestampFormatSeconds);
        case 'TIMESTAMP_MINUTES':
            return moment(value).format(timestampFormatMinutes);
        case 'USER':
            return value.displayName || value.fullname || value.username;
        case 'AUFTRAGPOSITION':
            return value.number;
        case 'GROUP':
            return value.name;
        case 'ADDRESSBOOK':
            return value
                .map(({ displayName }) => displayName)
                .join(', ');
        default:
    }
    if (result === undefined) {
        result = '???';
    }
    return result;
};

export default formatterFormat;
