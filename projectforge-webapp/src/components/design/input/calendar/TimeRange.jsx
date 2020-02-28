import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import InputContainer from '../InputContainer';
import styles from './CalendarInput.module.scss';
import TimeInput from './TimeInput';

function TimeRange(
    {
        from,
        hideDayPicker,
        id,
        label,
        onDelete,
        sameDate,
        setFrom,
        setTo,
        to,
    },
) {
    return (
        <div className={styles.timeRange}>
            <InputContainer
                className={styles.from}
                isActive
                // TODO TRANSLATION
                label={`${label || ''} [Von]`}
                withMargin
            >
                <TimeInput
                    setTime={setFrom}
                    time={from}
                    id={`time-input-${id}-from`}
                    showDate
                />
            </InputContainer>
            <InputContainer
                className={styles.to}
                isActive
                // TODO TRANSLATION
                label="[Bis]"
                withMargin
            >
                <TimeInput
                    hideDayPicker={hideDayPicker}
                    id={`time-input-${id}-from`}
                    setTime={setTo}
                    showDate={!sameDate}
                    time={to}
                />
                {/* TODO ENABLE DELETE BUTTON */}
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
    label: PropTypes.string,
    onDelete: PropTypes.func,
    sameDate: PropTypes.bool,
    to: PropTypes.instanceOf(Date),
};

TimeRange.defaultProps = {
    from: undefined,
    hideDayPicker: true,
    label: undefined,
    onDelete: undefined,
    sameDate: false,
    to: undefined,
};

export default TimeRange;
