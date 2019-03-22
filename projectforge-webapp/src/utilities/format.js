const USER_FORMATTER = 'user';
const TIMESTAMP_MINUTES_FORMATTER = 'timestamp-minutes';

const format = (formatter, data) => {
    switch (formatter) {
        case USER_FORMATTER:
            return data.fullname;
        case TIMESTAMP_MINUTES_FORMATTER:
            // TODO: GET DATE FORMAT FROM SERVER
            return new Date(Date.parse(data)).toLocaleString();
        default:
            return data;
    }
};

export default format;
