import moment from 'moment';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import React from 'react';
import DayPicker from 'react-day-picker';
import MomentLocaleUtils from 'react-day-picker/moment';
import { connect } from 'react-redux';
import { colorPropType } from '../../../../utilities/propTypes';
import AdvancedPopper from '../../popper/AdvancedPopper';
import AdditionalLabel from '../AdditionalLabel';
import InputContainer from '../InputContainer';
import styles from './CalendarInput.module.scss';

function DateInput(
    {
        additionalLabel,
        color,
        hideDayPicker,
        jsDateFormat,
        label,
        locale,
        noInputContainer,
        setDate,
        todayButton,
        value,
    },
) {
    const [inputValue, setInputValue] = React.useState('');
    const [isActive, setIsActive] = React.useState(false);
    const [isOpen, setIsOpen] = React.useState(false);
    const inputRef = React.useRef(null);
    const Tag = noInputContainer ? React.Fragment : InputContainer;

    React.useEffect(() => {
        if (value) {
            setInputValue(moment(value)
                .format(jsDateFormat));
        } else {
            setInputValue('');
        }
    }, [value]);

    const handleBlur = () => {
        setIsActive(false);

        if (inputValue.trim() === '') {
            setDate(undefined);
            return;
        }

        const momentDate = moment(inputValue, jsDateFormat);

        if (momentDate.isValid()) {
            setDate(momentDate.toDate());
        } else {
            setInputValue(moment(value)
                .format(jsDateFormat));
        }
    };

    const handleChange = ({ target }) => {
        setInputValue(target.value);

        // Has to be strict, so moment doesnt correct your input every time you time
        const momentDate = moment(target.value, jsDateFormat, true);

        if (momentDate.isValid()) {
            setDate(momentDate.toDate());
        }
    };

    const handleFocus = () => setIsActive(true);

    const handleKeyDown = (event) => {
        const momentDate = moment(inputValue, jsDateFormat, true);

        if (momentDate.isValid()) {
            let newDate;
            if (event.key === 'ArrowUp') {
                newDate = momentDate.add(1, 'd');
            } else if (event.key === 'ArrowDown') {
                newDate = momentDate.subtract(1, 'd');
            }

            if (newDate) {
                event.preventDefault();
                setDate(newDate.toDate());
            }
        }
    };

    const handleTagClick = () => {
        if (inputRef.current) {
            inputRef.current.focus();
        }
    };

    const tagProps = {};

    if (Tag !== React.Fragment) {
        tagProps.color = color;
        tagProps.isActive = isActive || inputValue !== '';
        tagProps.label = label;
        tagProps.onClick = handleTagClick;
        tagProps.withMargin = true;
    }

    const placeholder = jsDateFormat
        .split('')
        .filter((char, index) => index >= inputValue.length)
        .join('');

    const input = (
        <>
            <Tag {...tagProps}>
                <div className={styles.dateInput}>
                    {isActive && (
                        <span
                            className={styles.placeholder}
                            style={{ left: `${jsDateFormat.length - placeholder.length}ch` }}
                        >
                            {placeholder}
                        </span>
                    )}
                    <input
                        ref={inputRef}
                        onBlur={handleBlur}
                        onChange={handleChange}
                        onFocus={handleFocus}
                        onKeyDown={handleKeyDown}
                        style={{ minWidth: `${jsDateFormat.length + 1}ch` }}
                        type="text"
                        value={inputValue}
                    />
                </div>
            </Tag>
            <AdditionalLabel title={additionalLabel} />
        </>
    );

    if (hideDayPicker) {
        return input;
    }

    const handleDayPickerClick = (day, { selected }) => {
        setDate(selected ? undefined : day);

        setIsOpen(false);
    };

    return (
        <AdvancedPopper
            basic={input}
            setIsOpen={setIsOpen}
            isOpen={isOpen}
            withInput
        >
            <DayPicker
                selectedDays={value}
                onDayClick={handleDayPickerClick}
                month={value}
                locale={locale}
                localeUtils={MomentLocaleUtils}
                onTodayButtonClick={setDate}
                todayButton={todayButton}
            />
        </AdvancedPopper>
    );
}

DateInput.propTypes = {
    jsDateFormat: PropTypes.string.isRequired,
    setDate: PropTypes.func.isRequired,
    additionalLabel: PropTypes.string,
    color: colorPropType,
    hideDayPicker: PropTypes.bool,
    label: PropTypes.string,
    locale: PropTypes.string,
    noInputContainer: PropTypes.bool,
    todayButton: PropTypes.string,
    value: PropTypes.instanceOf(Date),
};

DateInput.defaultProps = {
    additionalLabel: undefined,
    color: undefined,
    hideDayPicker: false,
    label: undefined,
    locale: 'en',
    noInputContainer: false,
    todayButton: undefined,
    value: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(DateInput);
