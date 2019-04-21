import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { connect } from 'react-redux';
import CustomizedLayout from './page/layout/customized';

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
        variables,
        id,
        dataType,
        dateFormat,
        timestampFormatMinutes,
    },
) {
    if (dataType === 'CUSTOMIZED') {
        return <CustomizedLayout id={id} data={data} variables={variables} />;
    }
    const value = Object.getByString(data, id);
    if (!value) {
        return <React.Fragment />;
    }
    if (formatter) {
        let result;
        switch (formatter) {
            case COST2_FORMATTER:
                result = value.formattedNumber;
                break;
            case CUSTOMER_FORMATTER:
                result = value.name;
                break;
            case DATE_FORMATTER:
                result = moment(value).format(dateFormat);
                break;
            case PROJECT_FORMATTER:
                result = value.name;
                break;
            case TASK_FORMATTER:
                result = value.title;
                break;
            case TIMESTAMP_MINUTES_FORMATTER:
                result = moment(value).format(timestampFormatMinutes);
                break;
            case USER_FORMATTER:
                result = value.fullname;
                break;
            default:
                result = value;
        }
        return <React.Fragment>{result}</React.Fragment>;
    }
    let result = value;
    if (dataType) {
        switch (dataType) {
            case 'DATE':
                result = moment(value).format(timestampFormatMinutes);
                break;
            default:
        }
    }
    return <React.Fragment>{result}</React.Fragment>;
}

Formatter.propTypes = {
    data: PropTypes.shape({}),
    variables: PropTypes.shape({}),
    id: PropTypes.string,
    formatter: PropTypes.string,
    dataType: PropTypes.string,
    dateFormat: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
};

Formatter.defaultProps = {
    data: undefined,
    variables: undefined,
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
