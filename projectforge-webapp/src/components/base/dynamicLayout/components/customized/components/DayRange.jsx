import timezone from 'moment-timezone';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import TimePicker from 'rc-time-picker';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import 'react-day-picker/lib/style.css';
import MomentLocaleUtils, { formatDate, parseDate } from 'react-day-picker/moment';
import { connect } from 'react-redux';
import AdditionalLabel from '../../../../../design/input/AdditionalLabel';
import style from '../../../../../design/input/Input.module.scss';
import { DynamicLayoutContext } from '../../../context';

/**
 * Range of day for time sheets.
 */
function DayRange(
    {
        additionalLabel,
        dateFormat,
        locale,
        timeNotation,
        values,
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const { startDateId, endDateId, label } = values;

    const resolveDate = (id) => {
        const dateEpochSeconds = Object.getByString(data, id);
        return dateEpochSeconds ? timezone(new Date(dateEpochSeconds)) : undefined;
    };

    const [startDate, setStartDate] = React.useState(resolveDate(startDateId));
    const [endDate, setEndDate] = React.useState(resolveDate(endDateId));

    const setFields = (newStartDate, newEndDate) => {
        newEndDate.set({
            year: newStartDate.year(),
            dayOfYear: newStartDate.dayOfYear(),
            second: 0,
            millisecond: 0,
        });

        const endDayTime = newEndDate.hours() * 60 + newEndDate.minutes();
        const startDayTime = newStartDate.hours() * 60;

        if (endDayTime < startDayTime) {
            // Assume next day for endDate.
            newEndDate.add(1, 'days');
        }

        setStartDate(newStartDate);
        setEndDate(newEndDate);
        setData({
            [startDateId]: newStartDate.toDate(),
            [endDateId]: newEndDate.toDate(),
        });
    };

    // Sets the start date to the selected date by preserving time of day. Calls setFields as well.
    const changeDay = (value) => {
        const newStartDate = timezone(value);
        newStartDate.set({
            hour: startDate.hours(),
            minute: startDate.minutes(),
            second: 0,
            millisecond: 0,
        });

        setFields(newStartDate, endDate);
    };

    const changeStartTime = value => setFields(value, endDate);
    const changeEndTime = value => setFields(startDate, value);

    return (
        <React.Fragment>
            <span className={style.text}>{label}</span>
            <DayPickerInput
                formatDate={formatDate}
                parseDate={parseDate}
                format={dateFormat}
                value={startDate ? startDate.toDate() : undefined}
                onDayChange={changeDay}
                dayPickerProps={{
                    locale,
                    localeUtils: MomentLocaleUtils,
                }}
            />
            <TimePicker
                value={startDate}
                showSecond={false}
                minuteStep={15}
                allowEmpty={false}
                use12Hours={timeNotation === 'H12'}
                onChange={changeStartTime}
            />
            {ui.translations.until}
            <TimePicker
                value={endDate}
                showSecond={false}
                minuteStep={15}
                allowEmpty={false}
                use12Hours={timeNotation === 'H12'}
                onChange={changeEndTime}
            />
            <AdditionalLabel title={additionalLabel} />
        </React.Fragment>
    );
}

DayRange.propTypes = {
    dateFormat: PropTypes.string.isRequired,
    values: PropTypes.shape({
        startDateId: PropTypes.string,
        endDateId: PropTypes.string,
        label: PropTypes.string,
    }).isRequired,
    additionalLabel: PropTypes.string,
    locale: PropTypes.string,
    timeNotation: PropTypes.string,
};

DayRange.defaultProps = {
    additionalLabel: undefined,
    locale: 'en',
    timeNotation: 'H24',
};

const mapStateToProps = ({ authentication }) => ({
    dateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DayRange);
