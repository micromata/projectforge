import timezone from 'moment-timezone';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import React from 'react';
import DayPickerInput from 'react-day-picker/DayPickerInput';
import 'react-day-picker/lib/style.css';
import MomentLocaleUtils, { formatDate, parseDate } from 'react-day-picker/moment';
import { connect } from 'react-redux';
import AdditionalLabel from '../../../../design/input/AdditionalLabel';
import style from '../../../../design/input/Input.module.scss';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicDateInput(props) {
    const {
        additionalLabel,
        focus,
        id,
        jsDateFormat,
        label,
        locale,
        required,
    } = props;

    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const value = Object.getByString(data, id);

    return React.useMemo(() => {
        const handleDayChange = day => setData({
            [id]: day ? timezone(day)
                .format('YYYY-MM-DD') : null,
        });

        return (
            <React.Fragment>
                <span className={style.dayPickerLabel}>{label}</span>
                <DynamicValidationManager id={id}>
                    <DayPickerInput
                        autoFocus={focus}
                        formatDate={formatDate}
                        parseDate={parseDate}
                        format={jsDateFormat}
                        value={value}
                        onDayChange={handleDayChange}
                        dayPickerProps={{
                            locale,
                            localeUtils: MomentLocaleUtils,
                            todayButton: ui.translations['calendar.today'],
                        }}
                        placeholder={jsDateFormat}
                        required={required}
                    />
                    <AdditionalLabel title={additionalLabel} />
                </DynamicValidationManager>
            </React.Fragment>
        );
    }, [props, value, setData]);
}

DynamicDateInput.propTypes = {
    id: PropTypes.string.isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    focus: PropTypes.bool,
    locale: PropTypes.string,
    required: PropTypes.bool,
};

DynamicDateInput.defaultProps = {
    additionalLabel: undefined,
    focus: false,
    locale: 'en',
    required: false,
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(DynamicDateInput);
