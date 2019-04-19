import PropTypes from 'prop-types';
import moment from 'moment';
import {connect} from "react-redux";

const DATE_FORMATTER = 'DATE';
const COST2_FORMATTER = 'COST2';
const CUSTOMER_FORMATTER = 'CUSTOMER';
const PROJECT_FORMATTER = 'PROJECT';
const USER_FORMATTER = 'USER';
const TASK_FORMATTER = 'TASK_PATH';
const TIMESTAMP_MINUTES_FORMATTER = 'TIMESTAMP_MINUTES';

function Formatter(
    {
        formatter,
        data,
        dateFormat,
        timestampFormatMinutes
    },
) {
    if (!data) {
        return '';
    }
    switch (formatter) {
        case COST2_FORMATTER:
            return data.formattedNumber;
        case CUSTOMER_FORMATTER:
            return data.name;
        case DATE_FORMATTER:
            return moment(data).format(dateFormat);
        case PROJECT_FORMATTER:
            return data.name;
        case TASK_FORMATTER:
            return data.title;
        case TIMESTAMP_MINUTES_FORMATTER:
            return moment(data).format(timestampFormatMinutes);
        case USER_FORMATTER:
            return data.fullname;
        default:
            return data;
    }
}

Formatter.propTypes = {
    formatter: PropTypes.string,
    data: PropTypes.shape({}),
};

Formatter.defaultProps = {
    formatter: undefined,
};

const mapStateToProps = ({authentication}) => ({
    dateFormat: authentication.user.jsDateFormat,
    timestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(Formatter);
