import PropTypes from 'prop-types';
import React, {Component} from 'react';
import 'moment/min/locales';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import 'react-day-picker/lib/style.css';
import MomentLocaleUtils, { formatDate, parseDate } from 'react-day-picker/moment';
import {connect} from "react-redux";
import style from "../../../../../design/input/Input.module.scss";
import AdditionalLabel from "../../../../../design/input/AdditionalLabel";

/**
 * Range of day for tiem sheets.
 */
class DayRange extends Component {
    constructor(props) {
        super(props);

        const { data } = props;

        this.state = {
            day : data,
            startTime : undefined,
            stopTime : undefined // might be a time of the following day.
        };
    }

    render() {
        const {
            data,
            jsDateFormat: dateFormat,
            startDateId,
            endDateId,
            label,
            additionalLabel,
            locale,
            validation,
        } = this.props;

        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <DayPickerInput
                    formatDate={formatDate}
                    parseDate={parseDate}
                    format={dateFormat}
                    value={this.state.day ? formatDate(this.state.day, dateFormat) : undefined}
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
                    value={this.state.startTime}
                />
                <input
                    type="text"
                    id={endDateId}
                    value={this.state.stopTime}
                />
                <AdditionalLabel title={additionalLabel} />
            </React.Fragment>
        );
    }
}

DayRange.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    startDateId: PropTypes.string.isRequired,
    endDateId: PropTypes.string.isRequired,
    label: PropTypes.string,
    additionalLabel: PropTypes.string,
    validation: {},
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(DayRange);
