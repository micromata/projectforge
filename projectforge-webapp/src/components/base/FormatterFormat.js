import moment from 'moment';
import { isObject } from 'lodash/lang';

/**
 * Normalizes currency symbols to ISO 4217 currency codes.
 * Intl.NumberFormat requires ISO codes (EUR, USD) not symbols (€, $).
 *
 * @param {string} currency - Currency symbol or ISO code
 * @returns {string} ISO 4217 currency code
 */
const normalizeCurrency = (currency) => {
    if (!currency) {
        return 'EUR'; // Default fallback
    }

    // If already an ISO code (3 uppercase letters), return as-is
    if (/^[A-Z]{3}$/.test(currency)) {
        return currency;
    }

    // Map common currency symbols to ISO codes
    const symbolToCode = {
        '€': 'EUR',
        $: 'USD',
        '£': 'GBP',
        '¥': 'JPY',
        '₹': 'INR',
        '₽': 'RUB',
        '₣': 'CHF',
        Fr: 'CHF',
        'Fr.': 'CHF',
    };

    return symbolToCode[currency] || 'EUR'; // Fallback to EUR if unknown
};

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
                currency: normalizeCurrency(currency),
            }).format(value);
        case 'CURRENCY_PLAIN':
            return Intl.NumberFormat(locale, {
                style: 'decimal',
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
            }).format(value);
        case 'NUMBER':
            return Intl.NumberFormat(locale).format(value);
        case 'PERCENTAGE': {
            // Display percentage with decimal places only if they exist
            // e.g., 19 -> "19 %", 19.5 -> "19.5 %", 19.25 -> "19.25 %"
            if (value == null || Number.isNaN(value)) {
                return '???';
            }
            const hasDecimals = value % 1 !== 0;
            return Intl.NumberFormat(locale, {
                style: 'percent',
                minimumFractionDigits: 0,
                maximumFractionDigits: hasDecimals ? 2 : 0,
            }).format(value / 100);
        }
        case 'PERCENTAGE_DECIMAL': {
            // Display decimal percentage (e.g., 0.19 -> "19 %", 0.195 -> "19.5 %")
            if (value == null || Number.isNaN(value)) {
                return '???';
            }
            const hasDecimals = (value * 100) % 1 !== 0;
            return Intl.NumberFormat(locale, {
                style: 'percent',
                minimumFractionDigits: 0,
                maximumFractionDigits: hasDecimals ? 2 : 0,
            }).format(value); // No division by 100 - value is already decimal
        }
        case 'SHOW_DISPLAYNAME':
        case 'COST1':
        case 'COST2':
        case 'CUSTOMER':
        case 'KONTO':
        case 'PROJECT':
        case 'EMPLOYEE':
            return value?.displayName || value || '???';
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
            if (!value) return '';
            return moment(value).format(dateFormat);
        case 'TASK':
        case 'TASK_PATH':
            return value.title || value || '???';
        case 'TIMESTAMP':
            if (!value) return '';
            return moment(value).format(timestampFormatSeconds);
        case 'TIMESTAMP_MINUTES':
            if (!value) return '';
            return moment(value).format(timestampFormatMinutes);
        case 'USER':
            return value.displayName || value.fullname || value.username || value || '???';
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
