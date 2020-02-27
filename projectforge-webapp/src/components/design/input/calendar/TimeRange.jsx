import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
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
        <React.Fragment>
            <InputContainer
                className={classNames(styles.timeRange, styles.from)}
                isActive
                label={`${label || ''} [Von]`}
            >
                <TimeInput
                    setTime={setFrom}
                    time={from}
                    id={`time-input-${id}-from`}
                    showDate
                />
            </InputContainer>
            <InputContainer
                className={classNames(styles.timeRange, styles.to)}
                isActive
                label="[Bis]"
            >
                <TimeInput
                    hideDayPicker={hideDayPicker}
                    id={`time-input-${id}-from`}
                    setTime={setTo}
                    showDate={sameDate}
                    time={to}
                />
                {/* TODO ENABLE DELETE BUTTON */}
                {false && (
                    <FontAwesomeIcon
                        icon={faTimes}
                        onClick={onDelete}
                    />
                )}
            </InputContainer>
        </React.Fragment>
    );
}

TimeRange.propTypes = {
    from: PropTypes.instanceOf(Date).isRequired,
    id: PropTypes.string.isRequired,
    setFrom: PropTypes.func.isRequired,
    setTo: PropTypes.func.isRequired,
    to: PropTypes.instanceOf(Date).isRequired,
    hideDayPicker: PropTypes.bool,
    label: PropTypes.string,
    onDelete: PropTypes.func,
    sameDate: PropTypes.bool,
};

TimeRange.defaultProps = {
    hideDayPicker: true,
    label: undefined,
    onDelete: undefined,
    sameDate: false,
};

export default TimeRange;
