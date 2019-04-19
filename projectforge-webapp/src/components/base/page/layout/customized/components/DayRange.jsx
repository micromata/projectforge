import PropTypes from 'prop-types';
import React, { Component } from 'react';
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
        const { startDateId } = values;

        this.state = {
            day: startDateId ? Object.getByString(data, startDateId) : undefined,
            startTime: undefined,
            stopTime: undefined, // might be a time of the following day.
        };
        this.onStartTimeChange = this.onStartTimeChange.bind(this);
    }

    onStartTimeChange(value) {
        //console.log(value && value.format('HH:mm'));
    }

    render() {
        const {
            jsDateFormat: dateFormat,
            timeNotation,
            locale,
            values,
            additionalLabel,
            translations,
        } = this.props;
        const { label, startDateId, endDateId } = values;

        const { day, startTime, stopTime } = this.state;

        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <DayPickerInput
                    formatDate={formatDate}
                    parseDate={parseDate}
                    format={dateFormat}
                    value={day ? formatDate(day, dateFormat) : undefined}
                    onDayChange={this.handleDayChange}
                    dayPickerProps={{
                        locale,
                        localeUtils: MomentLocaleUtils,
                    }}
                    placeholder={dateFormat}
                />
                <TimePicker
                    showSecond={false}
                    minuteStep={5}
                    allowEmpty={false}
                    use12Hours={timeNotation === 'H12'}
                    onChange={this.onStartTimeChange}
                />
                ' '{translations.until}' '
                <TimePicker
                    showSecond={false}
                    minuteStep={15}
                    allowEmpty={false}
                    use12Hours={timeNotation === 'H12'}
                />
                <AdditionalLabel title={additionalLabel} />
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
    jsDateFormat: PropTypes.string.isRequired,
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
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DayRange);
