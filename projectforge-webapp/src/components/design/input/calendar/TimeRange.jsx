import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import InputContainer from '../InputContainer';
import styles from './CalendarInput.module.scss';
import DateTimeInput from './DateTimeInput';

function TimeRange(
    {
        from,
        hideDayPicker = false,
        hideTimeInput = false,
        id,
        label,
        onDelete,
        sameDate = false,
        setFrom,
        setTo,
        to,
        toLabel,
    },
) {
    return (
        <div className={styles.timeRange}>
            <InputContainer
                className={styles.from}
                isActive
                label={label}
                withMargin
            >
                <DateTimeInput
                    hideDayPicker={hideDayPicker}
                    hideTimeInput={hideTimeInput}
                    id={`time-input-${id}-from`}
                    setTime={setFrom}
                    showDate
                    time={from}
                />
            </InputContainer>
            <InputContainer
                className={styles.to}
                isActive
                label={toLabel || 'until'}
                withMargin
            >
                <DateTimeInput
                    hideDayPicker={hideDayPicker}
                    hideTimeInput={hideTimeInput}
                    id={`time-input-${id}-from`}
                    setTime={setTo}
                    showDate={!sameDate}
                    time={to}
                />
                {onDelete && (
                    <FontAwesomeIcon
                        icon={faTimes}
                        onClick={onDelete}
                        className={styles.deleteButton}
                    />
                )}
            </InputContainer>
        </div>
    );
}

TimeRange.propTypes = {
    id: PropTypes.string.isRequired,
    setFrom: PropTypes.func.isRequired,
    setTo: PropTypes.func.isRequired,
    from: PropTypes.instanceOf(Date),
    hideDayPicker: PropTypes.bool,
    hideTimeInput: PropTypes.bool,
    label: PropTypes.string,
    onDelete: PropTypes.func,
    sameDate: PropTypes.bool,
    to: PropTypes.instanceOf(Date),
    toLabel: PropTypes.string,
};

export default TimeRange;
