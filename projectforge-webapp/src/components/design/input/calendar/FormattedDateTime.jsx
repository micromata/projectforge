import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';

function FormattedDateTime(
    {
        date,
        jsDateFormat,
        jsTimestampFormatMinutes,
        slot,
    },
) {
    let format = jsTimestampFormatMinutes;

    if (
        (slot === 'FROM' && date.getHours() === 0 && date.getMinutes() === 0)
        || (slot === 'TO' && date.getHours() === 23 && date.getMinutes() === 59)) {
        format = jsDateFormat;
    }

    return (
        <React.Fragment>
            {moment(date)
                .format(format)}
        </React.Fragment>
    );
}

FormattedDateTime.propTypes = {
    date: PropTypes.instanceOf(Date).isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    jsTimestampFormatMinutes: PropTypes.string.isRequired,
    slot: PropTypes.oneOf(['FROM', 'TO']),
};

FormattedDateTime.defaultProps = {
    slot: 'FROM',
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    jsTimestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
});

export default connect(mapStateToProps)(FormattedDateTime);
