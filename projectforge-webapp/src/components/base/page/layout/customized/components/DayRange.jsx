import PropTypes from 'prop-types';
import React, { Component } from 'react';
import 'moment/min/locales';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import 'react-day-picker/lib/style.css';
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
    }

    render() {
        const {
            jsDateFormat: dateFormat,
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
                <input
                    type="text"
                    id={startDateId}
                    value={startTime}
                />
                {translations['until']}
                <input
                    type="text"
                    id={endDateId}
                    value={stopTime}
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
    additionalLabel: PropTypes.string,
    translations: PropTypes.arrayOf(PropTypes.string),
    validation: {},
};

DayRange.defaultProps = {
    additionalLabel: undefined,
    locale: 'en',
    translations: [],
    validation: {},
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(DayRange);
