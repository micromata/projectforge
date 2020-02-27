import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import DayPicker from 'react-day-picker';
import { connect } from 'react-redux';
import AdvancedPopper from '../../popper/AdvancedPopper';
import InputContainer from '../InputContainer';
import styles from './CalendarInput.module.scss';

function DateInput(
    {
        date,
        hideDayPicker,
        jsDateFormat,
        noInputContainer,
        setDate,
    },
) {
    const [inputValue, setInputValue] = React.useState('');
    const [isActive, setIsActive] = React.useState(false);
    const [isOpen, setIsOpen] = React.useState(false);
    const inputRef = React.useRef(null);
    const Tag = noInputContainer ? React.Fragment : InputContainer;

    React.useEffect(() => {
        if (date) {
            setInputValue(moment(date)
                .format(jsDateFormat));
        } else {
            setInputValue('');
        }
    }, [date]);

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
            setInputValue(moment(date)
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
        tagProps.onClick = handleTagClick;
        tagProps.isActive = isActive;
    }

    const input = (
        <Tag {...tagProps}>
            <div className={styles.dateInput}>
                <span className={styles.placeholder}>
                    {jsDateFormat
                        .split('')
                        .filter((char, index) => index >= inputValue.length)
                        .join('')}
                </span>
                <input
                    ref={inputRef}
                    onBlur={handleBlur}
                    onChange={handleChange}
                    onFocus={handleFocus}
                    onKeyDown={handleKeyDown}
                    size={jsDateFormat.length}
                    type="text"
                    value={inputValue}
                />
            </div>
        </Tag>
    );

    if (hideDayPicker) {
        return input;
    }

    const handleDayPickerClick = (day, { selected }) => setDate(selected ? undefined : day);

    return (
        <AdvancedPopper
            basic={input}
            setIsOpen={setIsOpen}
            isOpen={isOpen}
            withInput
        >
            <DayPicker
                selectedDays={date}
                onDayClick={handleDayPickerClick}
                month={date}
            />
        </AdvancedPopper>
    );
}

DateInput.propTypes = {
    jsDateFormat: PropTypes.string.isRequired,
    setDate: PropTypes.func.isRequired,
    date: PropTypes.instanceOf(Date),
    hideDayPicker: PropTypes.bool,
    noInputContainer: PropTypes.bool,
};

DateInput.defaultProps = {
    date: undefined,
    hideDayPicker: false,
    noInputContainer: false,
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
});

export default connect(mapStateToProps)(DateInput);
