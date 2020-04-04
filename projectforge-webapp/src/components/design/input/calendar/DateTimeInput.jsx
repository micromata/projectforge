import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import DateInput from './DateInput';
import TimeInput, { PrecisionType } from './TimeInput';

function DateTimeInput(
    {
        hideDayPicker,
        hideTimeInput,
        jsDateFormat,
        precision,
        setTime,
        showDate,
        time,
    },
) {
    const handleDateChange = (newDate) => {
        const momentDate = moment(newDate, jsDateFormat);

        if (!momentDate.isValid()) {
            return;
        }

        // Manipulating with MomentJS
        momentDate.hour(time.getHours());
        momentDate.minute(time.getMinutes());

        setTime(momentDate.toDate());
    };

    const handleTimeChange = (newTime) => {
        const momentDate = moment(time);

        momentDate.hour(newTime[0]);
        momentDate.minute(newTime[1]);

        if (!momentDate.isValid()) {
            return;
        }

        setTime(momentDate.toDate());
    };

    return (
        <React.Fragment>
            {showDate && (
                <DateInput
                    hideDayPicker={hideDayPicker}
                    noInputContainer
                    setDate={handleDateChange}
                    value={time}
                />
            )}
            {!hideTimeInput && (
                <TimeInput
                    value={[time.getHours(), time.getMinutes()]}
                    onChange={handleTimeChange}
                    precision={precision}
                />
            )}
        </React.Fragment>
    );
}

DateTimeInput.propTypes = {
    jsDateFormat: PropTypes.string.isRequired,
    setTime: PropTypes.func.isRequired,
    hideDayPicker: PropTypes.bool,
    hideTimeInput: PropTypes.bool,
    precision: PrecisionType,
    showDate: PropTypes.bool,
    time: PropTypes.instanceOf(Date),
};

DateTimeInput.defaultProps = {
    hideDayPicker: false,
    hideTimeInput: false,
    precision: 5,
    showDate: false,
    time: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
});

export default connect(mapStateToProps)(DateTimeInput);
