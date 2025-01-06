import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';

function FormattedDateTime(
    {
        date,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        dispatch,
        jsDateFormat,
        jsTimestampFormatMinutes,
        slot = 'FROM',
        ...props
    },
) {
    let format = jsTimestampFormatMinutes;

    if (
        (slot === 'FROM' && date.getHours() === 0 && date.getMinutes() === 0)
        || (slot === 'TO' && date.getHours() === 23 && date.getMinutes() === 59)) {
        format = jsDateFormat;
    }

    return (
        <span {...props}>
            {moment(date)
                .format(format)}
        </span>
    );
}

FormattedDateTime.propTypes = {
    date: PropTypes.instanceOf(Date).isRequired,
    dispatch: PropTypes.func.isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    jsTimestampFormatMinutes: PropTypes.string.isRequired,
    slot: PropTypes.oneOf(['FROM', 'TO']),
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    jsTimestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(FormattedDateTime);
