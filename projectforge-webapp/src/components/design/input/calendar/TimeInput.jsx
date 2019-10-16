import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import AdvancedPopper from '../../popper/AdvancedPopper';
import style from './CalendarInput.module.scss';
import TimeInputUnit from './TimeInputUnit';

function TimeInput(
    {
        id,
        precision,
        setTime,
        time,
    },
) {
    // Redirect to TempTimeInput until it's not done.
    const redirect = true;

    const [hourInputState, setHourInputState] = React.useState(time.getHours());
    const [minuteInputState, setMinuteInputState] = React.useState(time.getMinutes());

    const [isOpen, setIsOpen] = React.useState(false);
    const hourRef = React.useRef(null);
    const minuteRef = React.useRef(null);

    const setHour = (hour) => {
        if (hour < 0 || hour > 23) {
            return;
        }

        const newTime = new Date(time);
        newTime.setHours(hour);

        setTime(newTime);
    };

    const setMinute = (minute) => {
        if (minute < 0 || minute > 59) {
            return;
        }

        const newTime = new Date(time);
        newTime.setMinutes(minute);

        setTime(newTime);
    };

    const handleHourChange = ({ target }) => {
        if (!/^([01]?[0-9]|2[0-3]|)$/.test(target.value)) {
            return;
        }

        setHourInputState(target.value);
    };

    const handleHourBlur = ({ target }) => {
        if (!/^([01]?[0-9]|2[0-3])$/.test(target.value)) {
            return;
        }

        setHour(target.value);
    };

    const handleMinuteChange = ({ target }) => {
        if (!/^([0-5]?[0-9]|)$/.test(target.value)) {
            return;
        }

        setMinuteInputState(target.value);
    };

    const handleMinuteKeyDown = ({ target, key }) => {
        if (target.value === '' && key === 'Backspace' && hourRef.current) {
            hourRef.current.focus();
        }
    };

    const handleMinuteBlur = ({ target }) => {
        if (!/^([0-5]?[0-9])$/.test(target.value)) {
            return;
        }

        setMinute(target.value);
    };

    if (redirect) {
        return (
            <React.Fragment>
                <input
                    className={style.tempTimeInput}
                    onBlur={handleHourBlur}
                    onChange={handleHourChange}
                    value={hourInputState}
                />
                <span>:</span>
                <input
                    className={style.tempTimeInput}
                    onBlur={handleMinuteBlur}
                    onChange={handleMinuteChange}
                    value={minuteInputState}
                />
            </React.Fragment>
        );
    }

    return (
        <AdvancedPopper
            basic={(
                <div onClick={() => setIsOpen(true)} role="presentation">
                    <input
                        ref={hourRef}
                        value={time.getHours()}
                        className={style.hourInput}
                        onChange={handleHourChange}
                    />
                    <span>:</span>
                    <input
                        ref={minuteRef}
                        className={style.minuteInput}
                        onChange={handleMinuteChange}
                        onKeyDown={handleMinuteKeyDown}
                        value={time.getMinutes()}
                    />
                </div>
            )}
            className={style.timeInput}
            isOpen={isOpen}
            setIsOpen={setIsOpen}
        >
            <ul className={style.hours}>
                {[...Array(12)
                    .keys()]
                    .map(hour => (
                        <TimeInputUnit
                            key={`time-input-${id}-hour-${hour}`}
                            className={style.hour}
                            selected={time.getHours()}
                            onClick={setHour}
                        >
                            {hour}
                        </TimeInputUnit>
                    ))}
            </ul>
            <ul className={style.hours}>
                {[...Array(12)
                    .keys()]
                    .map(hour => hour + 12)
                    .map(hour => (
                        <TimeInputUnit
                            key={`time-input-${id}-hour-${hour}`}
                            className={style.hour}
                            selected={time.getHours()}
                            onClick={setHour}
                        >
                            {hour}
                        </TimeInputUnit>
                    ))}
            </ul>
            {precision < 60 && 60 % precision === 0 && (
                <ul className={classNames(style.minutes, style[`precision-${precision}`])}>
                    {[...Array(60 / precision)
                        .keys()]
                        .map(minute => minute * precision)
                        .map(minute => (
                            <TimeInputUnit
                                key={`time-input-${id}-minute-${minute}`}
                                className={style.minute}
                                selected={time.getMinutes()}
                                onClick={setMinute}
                            >
                                {minute}
                            </TimeInputUnit>
                        ))}
                </ul>
            )}
        </AdvancedPopper>
    );
}

TimeInput.propTypes = {
    id: PropTypes.string.isRequired,
    setTime: PropTypes.func.isRequired,
    time: PropTypes.instanceOf(Date).isRequired,
    precision: PropTypes.oneOf([1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60]),
};

TimeInput.defaultProps = {
    precision: 5,
};

export default TimeInput;
