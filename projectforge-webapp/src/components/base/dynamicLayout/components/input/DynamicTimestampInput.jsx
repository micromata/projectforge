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
import AdditionalLabel from '../../../../design/input/AdditionalLabel';
import style from '../../../../design/input/Input.module.scss';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicTimestampInput(
    {
        additionalLabel,
        id,
        jsDateFormat,
        label,
        locale,
        timeNotation,
    },
) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const dateStr = Object.getByString(data, id);
    const [date, setDate] = React.useState(dateStr ? timezone(dateStr) : undefined);

    return React.useMemo(() => {
        const setFields = (newDate) => {
            setDate(newDate);
            setData({
                [id]: newDate.toDate(),
            });
        };

        const handleTimeChange = (value) => {
            if (value === undefined) {
                return;
            }

            setFields(value);
        };

        const handleDayChange = (value) => {
            const newDate = timezone(value);
            newDate.set({
                hour: date ? date.hours() : 0,
                minute: date ? date.minutes() : 0,
                second: 0,
                millisecond: 0,
            });
            setFields(newDate);
        };

        return (
            <React.Fragment>
                <span className={style.text}>{label}</span>
                <ValidationManager>
                    <DayPickerInput
                        formatDate={formatDate}
                        parseDate={parseDate}
                        format={jsDateFormat}
                        value={date ? date.toDate() : undefined}
                        onDayChange={handleDayChange}
                        dayPickerProps={{
                            locale,
                            localeUtils: MomentLocaleUtils,
                        }}
                        placeholder={jsDateFormat}
                    />
                    <TimePicker
                        value={date}
                        showSecond={false}
                        minuteStep={15}
                        allowEmpty={false}
                        use12Hours={timeNotation === 'H12'}
                        onChange={handleTimeChange()}
                    />
                    <AdditionalLabel title={additionalLabel} />
                </ValidationManager>
            </React.Fragment>
        );
    }, [date]);
}

DynamicTimestampInput.propTypes = {
    id: PropTypes.string.isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    label: PropTypes.string,
    locale: PropTypes.string,
    timeNotation: PropTypes.string,
};

DynamicTimestampInput.defaultProps = {
    additionalLabel: undefined,
    label: undefined,
    locale: 'en',
    timeNotation: 'H24',
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DynamicTimestampInput);
