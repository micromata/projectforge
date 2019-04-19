import PropTypes from 'prop-types';
import React, {Component} from 'react';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import MomentLocaleUtils, {formatDate, parseDate} from "react-day-picker/moment";

import 'react-day-picker/lib/style.css';
import {dataPropType} from '../../../../utilities/propTypes';
import style from "../../../design/input/Input.module.scss";
import AdditionalLabel from "../../../design/input/AdditionalLabel";
import {connect} from "react-redux";

import 'moment/min/locales';

class LayoutDayPickerInput extends Component {
    constructor(props) {
        super(props);

        this.handleInputChange = this.handleInputChange.bind(this);
        const {locale, jsDateFormat} = this.props;
        this.state = {
            locale: locale ? locale : 'de',
            dateFormat: jsDateFormat
        }
    }

    handleInputChange(event) {
        const {id, changeDataField} = this.props;

        if (!id || !changeDataField) {
            return;
        }
        changeDataField(id, event.target.value);
    }

    render() {
        const {
            additionalLabel,
            changeDataField,
            data,
            focus,
            id,
            label,
            required,
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

        return (<React.Fragment>
                <span className={style.text}>{label}</span>
                <DayPickerInput
                    formatDate={formatDate}
                    parseDate={parseDate}
                    format={this.state.dateFormat}
                    value={value ? formatDate(value, this.state.dateFormat) : undefined}
                    dayPickerProps={{
                        localeUtils: MomentLocaleUtils,
                        locale: "de",
                        todayButton: this.props.translations['calendar.today'],
                    }}
                    placeholder={this.state.dateFormat}
                />
                <AdditionalLabel title={additionalLabel}/>
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
    required: PropTypes.bool,
    validation: PropTypes.shape({}),
};

LayoutDayPickerInput.defaultProps = {
    additionalLabel: undefined,
    changeDataField: undefined,
    focus: false,
    id: undefined,
    required: false,
    validation: {},
};

const mapStateToProps = ({authentication}) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(LayoutDayPickerInput);
