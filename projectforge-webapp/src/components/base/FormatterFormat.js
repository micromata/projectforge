import moment from 'moment';
import React from 'react';
import { isObject } from 'lodash/lang';

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
        case 'CURRENCY':
            return Intl.NumberFormat(locale, {
                style: 'currency',
                currency,
            }).format(value);
        case 'SHOW_DISPLAYNAME':
        case 'COST1':
        case 'COST2':
        case 'CUSTOMER':
        case 'KONTO':
        case 'PROJECT':
        case 'EMPLOYEE':
            return value?.displayName ?? '???';
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
        case 'TASK_PATH':
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
        case 'ADDRESS_BOOK':
            return value
                .map(({ displayName }) => displayName)
                .join(', ');
        default:
    }
    if (result === undefined || isObject(result)) {
        result = '???';
    }
    return result;
};

export default formatterFormat;
