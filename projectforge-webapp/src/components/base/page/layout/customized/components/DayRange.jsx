import PropTypes from 'prop-types';
import React, { Component } from 'react';
import timezone from 'moment-timezone';
import 'moment/min/locales';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import 'react-day-picker/lib/style.css';
import 'rc-time-picker/assets/index.css';
import TimePicker from 'rc-time-picker';
import MomentLocaleUtils, { formatDate, parseDate } from 'react-day-picker/moment';
import { connect } from 'react-redux';
import style from '../../../../../design/input/Input.module.scss';
import AdditionalLabel from '../../../../../design/input/AdditionalLabel';

/**
 * Range of day for tiem sheets.
 */
class DayRange extends Component {
    constructor(props) {
        super(props);

        const { data, values } = props;
        const { startDateId, endDateId } = values;

        const startDateEpochSeconds = startDateId
            ? Object.getByString(data, startDateId) : undefined;
        const startDate = startDateEpochSeconds
            ? timezone(new Date(startDateEpochSeconds)) : undefined;
        const endDateEpochSeconds = endDateId
            ? Object.getByString(data, endDateId) : undefined;
        const endDate = endDateEpochSeconds
            ? timezone(endDateEpochSeconds) : undefined;
        this.state = {
            startDate,
            endDate, // might be a time of the following day or of the same day.
        };

        this.handleDayChange = this.handleDayChange.bind(this);
        this.onStartTimeChange = this.onStartTimeChange.bind(this);
        this.onEndTimeChange = this.onEndTimeChange.bind(this);
        this.setFields = this.setFields.bind(this);
    }

    onStartTimeChange(value) {
        if (value == null) {
            return;
        }
        const { endDate } = this.state;
        this.setFields(value, endDate);
    }

    onEndTimeChange(value) {
        if (value == null) {
            return;
        }
        const { startDate } = this.state;
        this.setFields(startDate, value);
    }

    setFields(startDate, endDate) {
        endDate.set({
            year: startDate.year(),
            dayOfYear: startDate.dayOfYear(),
            second: 0,
            millisecond: 0,
        });
        const endDayTime = endDate.hours() * 60 + endDate.minutes();
        const startDayTime = startDate.hours() * 60;
        if (endDayTime < startDayTime) {
            // Assume next day for endDate:
            endDate.add(1, 'days');
        }
        this.setState({
            startDate,
            endDate
        });
        const { changeDataField, values } = this.props;
        const { startDateId, endDateId } = values;
        changeDataField(startDateId, startDate.toDate());
        changeDataField(endDateId, endDate.toDate());
    }


    // Sets the start date to the selected date by preserving time of day. Calls setFields as well.
    handleDayChange(value) {
        const { startDate, endDate } = this.state;
        const newStartDate = timezone(value);
        newStartDate.set({
            hour: startDate.hours(),
            minute: startDate.minutes(),
            second: 0,
            millisecond: 0,
        });
        this.setFields(newStartDate, endDate);
    }

    render() {
        const {
            dateFormat,
            timeNotation,
            locale,
            values,
            additionalLabel,
            translations,
        } = this.props;
        const { label } = values;

        const { startDate, endDate } = this.state;
        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <DayPickerInput
                    formatDate={formatDate}
                    parseDate={parseDate}
                    format={dateFormat}
                    value={startDate.toDate()}
                    onDayChange={this.handleDayChange}
                    dayPickerProps={{
                        locale,
                        localeUtils: MomentLocaleUtils,
                    }}
                    placeholder={dateFormat}
                />
                {' '}
                <TimePicker
                    value={startDate}
                    showSecond={false}
                    minuteStep={15}
                    allowEmpty={false}
                    use12Hours={timeNotation === 'H12'}
                    onChange={this.onStartTimeChange}
                />
                {' '}
                {translations.until}
                {' '}
                <TimePicker
                    value={endDate}
                    showSecond={false}
                    minuteStep={15}
                    allowEmpty={false}
                    use12Hours={timeNotation === 'H12'}
                    onChange={this.onEndTimeChange}
                />
                <AdditionalLabel title={additionalLabel}/>
            </React.Fragment>
        );
    }
}

DayRange.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    values: PropTypes.shape({
        startDateId: PropTypes.string,
        endDateId: PropTypes.string,
        label: PropTypes.string,
    }).isRequired,
    dateFormat: PropTypes.string.isRequired,
    locale: PropTypes.string,
    timeNotation: PropTypes.string,
    additionalLabel: PropTypes.string,
    translations: PropTypes.shape({}).isRequired,
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
