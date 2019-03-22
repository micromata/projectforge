const USER_FORMATTER = 'user';

const format = (formatter, data) => {
    switch (formatter) {
        case USER_FORMATTER:
            return data.fullname;
        default:
            return data;
    }
};

export default format;
