import timezone from 'moment-timezone';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import 'react-day-picker/lib/style.css';
import { connect } from 'react-redux';
import AdditionalLabel from '../../../../../design/input/AdditionalLabel';
import TimeRange from '../../../../../design/input/calendar/TimeRange';
import { DynamicLayoutContext } from '../../../context';

/**
 * Range of day for time sheets.
 */
// TODO CHECK USED PROPS
function DayRange(
    {
        additionalLabel,
        dateFormat,
        id,
        locale,
        timeNotation,
        values,
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const { startDateId, endDateId, label } = values;

    const resolveDate = (dateId) => {
        const dateEpochSeconds = Object.getByString(data, dateId);
        return dateEpochSeconds ? timezone(new Date(dateEpochSeconds)) : undefined;
    };

    const [startDate, setStartDate] = React.useState(undefined);
    const [endDate, setEndDate] = React.useState(undefined);

    React.useEffect(() => {
        setStartDate(resolveDate(startDateId));
    }, [Object.getByString(data, startDateId)]);

    React.useEffect(() => {
        setEndDate(resolveDate(endDateId));
    }, [Object.getByString(data, endDateId)]);

    return React.useMemo(() => {
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

        // Sets the start date to the selected date by preserving time of day. Calls setFields as
        // well.
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

        const changeStartTime = value => setFields(timezone(value), endDate);
        const changeEndTime = value => setFields(startDate, timezone(value));

        return (
            <React.Fragment>
                <TimeRange
                    // TODO SHOW CALENDAR, TRANSLATION
                    label={label}
                    sameDate
                    setFrom={changeStartTime}
                    setTo={changeEndTime}
                    id={id}
                    from={startDate ? startDate.toDate() : undefined}
                    to={endDate ? endDate.toDate() : undefined}
                />
                <AdditionalLabel title={additionalLabel} />
            </React.Fragment>
        );
    }, [startDate, endDate, setData]);
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
