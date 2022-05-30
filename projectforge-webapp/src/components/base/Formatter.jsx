import { faStar } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';

const AUFTRAGPOSITION_FORMATTER = 'AUFTRAG_POSITION';
const ADDRESSBOOK_FORMATTER = 'ADDRESS_BOOK';
const COST1_FORMATTER = 'COST1';
const COST2_FORMATTER = 'COST2';
const CUSTOMER_FORMATTER = 'CUSTOMER';
const CURRENCY_FORMATTER = 'CURRENCY';
const DATE_FORMATTER = 'DATE';
const EMPLOYEE_FORMATTER = 'EMPLOYEE';
const KONTO_FORMATTER = 'KONTO';
const PROJECT_FORMATTER = 'PROJECT';
const USER_FORMATTER = 'USER';
const RATING = 'RATING';
const TASK_FORMATTER = 'TASK_PATH';
const TIMESTAMP_FORMATTER = 'TIMESTAMP';
const TIMESTAMP_MINUTES_FORMATTER = 'TIMESTAMP_MINUTES';
const GROUP_FORMATTER = 'GROUP';

function Formatter(
    {
        formatter,
        value,
        data,
        id,
        dataType,
        dateFormat,
        timestampFormatSeconds,
        timestampFormatMinutes,
        valueIconMap,
        locale,
        currency,
    },
) {
    const useValue = value || Object.getByString(data, id);
    if (useValue === undefined) {
        return null;
    }

    let result = useValue;
    const valueIconsPresent = valueIconMap && valueIconMap.length !== 0;
    const useFormatter = !valueIconsPresent && (formatter || dataType);

    if (useFormatter) {
        switch (useFormatter) {
            case COST1_FORMATTER:
                result = useValue.formattedNumber;
                break;
            case COST2_FORMATTER:
                result = useValue.longDisplayName || useValue.formattedNumber;
                break;
            case CURRENCY_FORMATTER:
                result = Intl.NumberFormat(locale, {
                    style: 'currency',
                    currency,
                }).format(useValue);
                break;
            case CUSTOMER_FORMATTER:
            case KONTO_FORMATTER:
            case PROJECT_FORMATTER:
            case EMPLOYEE_FORMATTER:
                result = useValue.displayName;
                break;
            case DATE_FORMATTER:
                result = moment(useValue)
                    .format(dateFormat);
                break;
            case TASK_FORMATTER:
                result = useValue.title;
                break;
            case TIMESTAMP_FORMATTER:
                result = moment(useValue)
                    .format(timestampFormatSeconds);
                break;
            case TIMESTAMP_MINUTES_FORMATTER:
                result = moment(useValue)
                    .format(timestampFormatMinutes);
                break;
            case USER_FORMATTER:
                result = useValue.displayName || useValue.fullname || useValue.username;
                break;
            case AUFTRAGPOSITION_FORMATTER:
                result = useValue.number;
                break;
            case GROUP_FORMATTER:
                result = useValue.name;
                break;
            case ADDRESSBOOK_FORMATTER:
                result = useValue
                    .map(({ displayName }) => displayName)
                    .join(', ');
                break;
            case RATING:
                if (useValue > 0) {
                    result = [...Array(useValue).keys()].map((v) => (
                        <FontAwesomeIcon
                            icon={faStar}
                            color="#ffc107"
                            key={v}
                        />
                    ));
                } else {
                    result = '-';
                }
                break;
            default:
        }
    } else if (dataType === 'DATE') {
        result = moment(useValue)
            .format(timestampFormatMinutes);
    } else if (valueIconsPresent) {
        const valueIcon = valueIconMap[useValue];

        if (valueIcon) {
            result = <FontAwesomeIcon icon={valueIcon} />;
        }
    }

    return result;
}

Formatter.propTypes = {
    data: PropTypes.shape({}),
    // eslint-disable-next-line react/forbid-prop-types
    value: PropTypes.any,
    dataType: PropTypes.string,
    dateFormat: PropTypes.string,
    id: PropTypes.string,
    formatter: PropTypes.string,
    timestampFormatSeconds: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
    locale: PropTypes.string,
    currency: PropTypes.string,
    valueIconMap: PropTypes.shape({
        length: PropTypes.number,
    }),
};

Formatter.defaultProps = {
    data: undefined,
    id: undefined,
    value: undefined,
    formatter: undefined,
    dataType: undefined,
    dateFormat: 'DD/MM/YYYY',
    timestampFormatSeconds: 'DD.MM.YYYY HH:mm:ss',
    timestampFormatMinutes: 'DD.MM.YYYY HH:mm',
    locale: undefined,
    currency: undefined,
    valueIconMap: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    dateFormat: authentication.user.jsDateFormat,
    timestampFormatSeconds: authentication.user.jsTimestampFormatSeconds,
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(Formatter);
