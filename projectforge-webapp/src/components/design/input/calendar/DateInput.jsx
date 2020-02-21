import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import styles from './CalendarInput.module.scss';

function DateInput(
    {
        date,
        jsDateFormat,
        setDate,
    },
) {
    const [inputValue, setInputValue] = React.useState('');

    React.useEffect(() => {
        setInputValue(moment(date)
            .format(jsDateFormat));
    }, [date]);

    const handleChange = ({ target }) => {
        setInputValue(target.value);

        // Has to be strict, so moment doesnt correct your input every time you time
        const momentDate = moment(target.value, jsDateFormat, true);

        if (momentDate.isValid()) {
            setDate(momentDate.toDate());
        }
    };

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

    return (
        <div className={styles.dateInput}>
            <span className={styles.placeholder}>
                {jsDateFormat
                    .split('')
                    .filter((char, index) => index >= inputValue.length)
                    .join('')}
            </span>
            <input
                onChange={handleChange}
                onKeyDown={handleKeyDown}
                size={jsDateFormat.length}
                type="text"
                value={inputValue}
            />
        </div>
    );
}

DateInput.propTypes = {
    date: PropTypes.instanceOf(Date).isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    setDate: PropTypes.func.isRequired,
};

DateInput.defaultProps = {};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
});

export default connect(mapStateToProps)(DateInput);
