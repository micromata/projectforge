import moment from 'moment';

const USER_FORMATTER = 'user';
const TIMESTAMP_MINUTES_FORMATTER = 'timestamp-minutes';
export const TEXT_SINCE_TIMESTAMP = 'TEXT_SINCE_TIMESTAMP';

const format = (formatter, data) => {
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
