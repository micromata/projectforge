import moment from 'moment';

const USER_FORMATTER = 'USER';
const TIMESTAMP_MINUTES_FORMATTER = 'TIMESTAMP_MINUTES';
export const TEXT_SINCE_TIMESTAMP = 'TEXT_SINCE_TIMESTAMP';

const format = (formatter, data) => {
    if (!data) {
        return '';
    }

    switch (formatter) {
        case USER_FORMATTER:
            return data.fullname;
        case TIMESTAMP_MINUTES_FORMATTER:
            // TODO: GET DATE FORMAT FROM SERVER
            return new Date(Date.parse(data)).toLocaleString();
        case TEXT_SINCE_TIMESTAMP:
            return `${moment
                .duration(new Date().getTime() - new Date(data).getTime(), 'ms')
                .humanize()} ago`;
        default:
            return data;
    }
};

export default format;
