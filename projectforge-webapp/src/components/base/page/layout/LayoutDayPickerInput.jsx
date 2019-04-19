import timezone from 'moment-timezone';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import React from 'react';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import 'react-day-picker/lib/style.css';
import MomentLocaleUtils, { formatDate, parseDate } from 'react-day-picker/moment';
import { connect } from 'react-redux';
import { dataPropType } from '../../../../utilities/propTypes';
import AdditionalLabel from '../../../design/input/AdditionalLabel';
import style from '../../../design/input/Input.module.scss';

class LayoutDayPickerInput extends React.Component {
    constructor(props) {
        super(props);

        this.handleDayChange = this.handleDayChange.bind(this);
    }

    handleDayChange(day) {
        const { id, changeDataField } = this.props;

        if (!id || !changeDataField) {
            return;
        }

        changeDataField(
            id,
            timezone(day)
                .format('YYYY-MM-DD'),
        );
    }

    render() {
        const {
            additionalLabel,
            data,
            jsDateFormat: dateFormat,
            focus,
            id,
            label,
            locale,
            required,
            translations,
            validation,
        } = this.props;

        const properties = {};
        const value = data[id] || '';


        if (required && !value) {
            properties.color = 'danger';
        }

        if (validation[id]) {
            properties.color = 'danger';
            properties.additionalLabel = validation[id];
        }

        if (focus) {
            properties.autoFocus = true;
        }

        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <DayPickerInput
                    formatDate={formatDate}
                    parseDate={parseDate}
                    format={dateFormat}
                    value={value ? formatDate(value, dateFormat) : undefined}
                    onDayChange={this.handleDayChange}
                    dayPickerProps={{
                        locale,
                        localeUtils: MomentLocaleUtils,
                        todayButton: translations['calendar.today'],
                    }}
                    placeholder={dateFormat}
                />
                <AdditionalLabel title={additionalLabel} />
            </React.Fragment>
        );
    }
}

LayoutDayPickerInput.propTypes = {
    data: dataPropType.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    changeDataField: PropTypes.func,
    focus: PropTypes.bool,
    id: PropTypes.string,
    jsDateFormat: PropTypes.string.isRequired,
    locale: PropTypes.string,
    required: PropTypes.bool,
    translations: PropTypes.arrayOf(PropTypes.string),
    validation: PropTypes.shape({}),
};

LayoutDayPickerInput.defaultProps = {
    additionalLabel: undefined,
    changeDataField: undefined,
    focus: false,
    id: undefined,
    locale: 'en',
    required: false,
    translations: [],
    validation: {},
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(LayoutDayPickerInput);
