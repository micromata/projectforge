import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';

const DATE_FORMATTER = 'DATE';
const COST1_FORMATTER = 'COST1';
const COST2_FORMATTER = 'COST2';
const CUSTOMER_FORMATTER = 'CUSTOMER';
const KONTO_FORMATTER = 'KONTO';
const PROJECT_FORMATTER = 'PROJECT';
const USER_FORMATTER = 'USER';
const TASK_FORMATTER = 'TASK_PATH';
const TIMESTAMP_MINUTES_FORMATTER = 'TIMESTAMP_MINUTES';
const AUFTRAGPOSITION_FORMATTER = 'AUFTRAG_POSITION';
const GROUP_FORMATTER = 'GROUP';

function Formatter(
    {
        formatter,
        data,
        id,
        dataType,
        dateFormat,
        timestampFormatMinutes,
    },
) {
    const value = Object.getByString(data, id);
    if (!value) {
        return <React.Fragment />;
    }

    let result = value;

    // TODO FORMAT NUMBERS RIGHT ALIGNED
    if (formatter) {
        switch (formatter) {
            case COST1_FORMATTER:
                result = value.formattedNumber;
                break;
            case COST2_FORMATTER:
                result = value.formattedNumber;
                break;
            case CUSTOMER_FORMATTER:
                result = value.name;
                break;
            case KONTO_FORMATTER:
                result = `${value.nummer} - ${value.bezeichnung}`;
                break;
            case DATE_FORMATTER:
                result = moment(value)
                    .format(dateFormat);
                break;
            case PROJECT_FORMATTER:
                result = value.name;
                break;
            case TASK_FORMATTER:
                result = value.title;
                break;
            case TIMESTAMP_MINUTES_FORMATTER:
                result = moment(value)
                    .format(timestampFormatMinutes);
                break;
            case USER_FORMATTER:
                result = value.fullname;
                break;
            case AUFTRAGPOSITION_FORMATTER:
                result = value.number;
                break;
            case GROUP_FORMATTER:
                result = value.name;
                break;
            default:
        }
    } else if (dataType === 'DATE') {
        result = moment(value)
            .format(timestampFormatMinutes);
    }

    return result;
}

Formatter.propTypes = {
    data: PropTypes.shape({}),
    id: PropTypes.string,
    formatter: PropTypes.string,
    dataType: PropTypes.string,
    dateFormat: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
};

Formatter.defaultProps = {
    data: undefined,
    id: undefined,
    formatter: undefined,
    dataType: undefined,
    dateFormat: 'DD/MM/YYYY',
    timestampFormatMinutes: 'DD.MM.YYYY HH:mm',
};

const mapStateToProps = ({ authentication }) => ({
    dateFormat: authentication.user.jsDateFormat,
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(Formatter);
