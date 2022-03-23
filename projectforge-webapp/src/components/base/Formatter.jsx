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
        data,
        id,
        dataType,
        dateFormat,
        timestampFormatSeconds,
        timestampFormatMinutes,
        valueIconMap,
    },
) {
    const value = Object.getByString(data, id);
    if (value === undefined) {
        return null;
    }

    let result = value;
    const valueIconsPresent = valueIconMap && valueIconMap.length !== 0;
    const useFormatter = !valueIconsPresent && (formatter || dataType);

    // TODO FORMAT NUMBERS RIGHT ALIGNED
    if (useFormatter) {
        switch (useFormatter) {
            case COST1_FORMATTER:
            case COST2_FORMATTER:
                result = value.formattedNumber;
                break;
            case CUSTOMER_FORMATTER:
            case KONTO_FORMATTER:
            case PROJECT_FORMATTER:
            case EMPLOYEE_FORMATTER:
                result = value.displayName;
                break;
            case DATE_FORMATTER:
                result = moment(value)
                    .format(dateFormat);
                break;
            case TASK_FORMATTER:
                result = value.title;
                break;
            case TIMESTAMP_FORMATTER:
                result = moment(value)
                    .format(timestampFormatSeconds);
                break;
            case TIMESTAMP_MINUTES_FORMATTER:
                result = moment(value)
                    .format(timestampFormatMinutes);
                break;
            case USER_FORMATTER:
                result = value.displayName || value.fullname || value.username;
                break;
            case AUFTRAGPOSITION_FORMATTER:
                result = value.number;
                break;
            case GROUP_FORMATTER:
                result = value.name;
                break;
            case ADDRESSBOOK_FORMATTER:
                result = value
                    .map(({ displayName }) => displayName)
                    .join(', ');
                break;
            case RATING:
                if (value > 0) {
                    result = [...Array(value).keys()].map((v) => (
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
        result = moment(value)
            .format(timestampFormatMinutes);
    } else if (valueIconsPresent) {
        const valueIcon = valueIconMap[value];

        if (valueIcon) {
            result = <FontAwesomeIcon icon={valueIcon} />;
        }
    }

    return result;
}

Formatter.propTypes = {
    data: PropTypes.shape({}),
    dataType: PropTypes.string,
    dateFormat: PropTypes.string,
    id: PropTypes.string,
    formatter: PropTypes.string,
    timestampFormatSeconds: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
    valueIconMap: PropTypes.shape({}),
};

Formatter.defaultProps = {
    data: undefined,
    id: undefined,
    formatter: undefined,
    dataType: undefined,
    dateFormat: 'DD/MM/YYYY',
    timestampFormatSeconds: 'DD.MM.YYYY HH:mm:ss',
    timestampFormatMinutes: 'DD.MM.YYYY HH:mm',
    valueIconMap: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    dateFormat: authentication.user.jsDateFormat,
    timestampFormatSeconds: authentication.user.jsTimestampFormatSeconds,
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(Formatter);
