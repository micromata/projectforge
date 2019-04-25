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
import style from '../../../design/input/Input.module.scss';
import AdditionalLabel from '../../../design/input/AdditionalLabel';

class Timestamp extends Component {
    constructor(props) {
        super(props);

        const { data, id } = props;

        const dateStr = Object.getByString(data, id);
        const date = dateStr ? timezone(dateStr) : undefined;
        this.state = {
            date,
        };

        this.handleDayChange = this.handleDayChange.bind(this);
        this.onTimeChange = this.onTimeChange.bind(this);
        this.setFields = this.setFields.bind(this);
    }

    onTimeChange(value) {
        if (value == null) {
            return;
        }
        this.setFields(value);
    }

    setFields(date) {
        this.setState({ date });
        const { changeDataField, id } = this.props;
        changeDataField(id, date.toDate());
    }


    // Sets the start date to the selected date by preserving time of day. Calls setFields as well.
    handleDayChange(value) {
        const { date } = this.state;
        const newDate = timezone(value);
        newDate.set({
            hour: date ? date.hours() : 0,
            minute: date ? date.minutes() : 0,
            second: 0,
            millisecond: 0,
        });
        this.setFields(newDate);
    }

    render() {
        const {
            jsDateFormat: dateFormat,
            timeNotation,
            locale,
            label,
            additionalLabel,
        } = this.props;

        const { date } = this.state;
        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <DayPickerInput
                    formatDate={formatDate}
                    parseDate={parseDate}
                    format={dateFormat}
                    value={date ? date.toDate() : undefined}
                    onDayChange={this.handleDayChange}
                    dayPickerProps={{
                        locale,
                        localeUtils: MomentLocaleUtils,
                    }}
                    placeholder={dateFormat}
                />
                {' '}
                <TimePicker
                    value={date}
                    showSecond={false}
                    minuteStep={15}
                    allowEmpty={false}
                    use12Hours={timeNotation === 'H12'}
                    onChange={this.onStartTimeChange}
                />
                <AdditionalLabel title={additionalLabel} />
            </React.Fragment>
        );
    }
}

Timestamp.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    locale: PropTypes.string,
    timeNotation: PropTypes.string,
    label: PropTypes.string,
    additionalLabel: PropTypes.string,
};

Timestamp.defaultProps = {
    label: undefined,
    additionalLabel: undefined,
    locale: 'en',
    timeNotation: 'H24',
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(Timestamp);
