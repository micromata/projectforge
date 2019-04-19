import moment from 'moment';

const DATE_FORMATTER = 'DATE';
const COST2_FORMATTER = 'COST2';
const CUSTOMER_FORMATTER = 'CUSTOMER';
const PROJECT_FORMATTER = 'PROJECT';
const USER_FORMATTER = 'USER';
const TASK_FORMATTER = 'TASK_PATH';
const TIMESTAMP_MINUTES_FORMATTER = 'TIMESTAMP_MINUTES';

const format = (formatter, data, dateFormat, timestampFormatMinutes) => {
    if (!data) {
        return '';
    }
    switch (formatter) {
        case COST2_FORMATTER:
            return data.formattedNumber;
        case CUSTOMER_FORMATTER:
            return data.name;
        case PROJECT_FORMATTER:
            return data.name;
        case TASK_FORMATTER:
            return data.title;
        case TIMESTAMP_MINUTES_FORMATTER:
            // TODO: GET DATE FORMAT FROM SERVER
            return moment(data).format('DD.MM.YYYY hh:mm:ss');//timestampFormatMinutes); @Fin: hier gebraucht
        case DATE_FORMATTER:
            // TODO: GET DATE FORMAT FROM SERVER
            return moment(data).format('DD.MM.YYYY');//dateFormat); @Fin: und hier gebraucht
        case USER_FORMATTER:
            return data.fullname;
        default:
            return data;
    }
};

export default format;
