import moment from 'moment';

const DATE_FORMATTER = 'DATE';
const COST2_FORMATTER = 'COST2';
const CUSTOMER_FORMATTER = 'CUSTOMER';
const PROJECT_FORMATTER = 'PROJECT';
const USER_FORMATTER = 'USER';
const TASK_FORMATTER = 'TASK_PATH';
const TIMESTAMP_MINUTES_FORMATTER = 'TIMESTAMP_MINUTES';

export const TEXT_SINCE_TIMESTAMP = 'TEXT_SINCE_TIMESTAMP';

const format = (formatter, data) => {
    if (!data) {
        return '';
    }

    switch (formatter) {
        case COST2_FORMATTER:
            return data.formattedNumber;
        case CUSTOMER_FORMATTER:
            return data.name;
        case DATE_FORMATTER:
            return data.toLocaleString();
        case PROJECT_FORMATTER:
            return data.name;
        case TASK_FORMATTER:
            return data.title;
        case TEXT_SINCE_TIMESTAMP:
            return `${moment
                .duration(new Date().getTime() - new Date(data).getTime(), 'ms')
                .humanize()} ago`;
        case TIMESTAMP_MINUTES_FORMATTER:
            // TODO: GET DATE FORMAT FROM SERVER
            return new Date(Date.parse(data)).toLocaleString();
        case USER_FORMATTER:
            return data.fullname;
        default:
            return data;
    }
};

export default format;
