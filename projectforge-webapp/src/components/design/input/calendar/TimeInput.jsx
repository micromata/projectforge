import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { formatTimeUnit } from '../../../../utilities/layout';
import AdvancedPopper from '../../popper/AdvancedPopper';
import style from './CalendarInput.module.scss';
import TimeInputUnit from './TimeInputUnit';

function TimeInput({
    id,
    value,
    onChange,
    precision,
}) {
    const [isOpen, setIsOpen] = React.useState(false);

    const hourRef = React.useRef(null);
    const minuteRef = React.useRef(null);

    const hours = (value && value[0]) || 0;
    const minutes = (value && value[1]) || 0;

    const setHour = (input) => {
        const newHours = Number(input);

        if (newHours < 0 || newHours > 23) {
            return;
        }

        onChange([newHours, minutes]);
    };

    const setMinute = (input) => {
        const newMinutes = Number(input);

        if (newMinutes < 0 || newMinutes > 59) {
            return;
        }

        onChange([hours, newMinutes]);
    };

    const handleInputFocus = ({ target }) => {
        setIsOpen(true);
        target.select();
    };

    return (
        <AdvancedPopper
            basic={(
                <div
                    onClick={() => setIsOpen(true)}
                    role="presentation"
                    className={style.container}
                >
                    <input
                        className={style.hourInput}
                        ref={hourRef}
                        max={23}
                        min={0}
                        onChange={(e) => setHour(e.currentTarget.value)}
                        onFocus={handleInputFocus}
                        type="number"
                        value={formatTimeUnit(hours)}
                    />
                    <span>:</span>
                    <input
                        className={style.minuteInput}
                        ref={minuteRef}
                        max={59}
                        min={0}
                        onChange={(e) => setMinute(e.currentTarget.value)}
                        onFocus={handleInputFocus}
                        step={precision}
                        type="number"
                        value={formatTimeUnit(minutes)}
                    />
                </div>
            )}
            className={style.timeInput}
            isOpen={isOpen}
            setIsOpen={setIsOpen}
            withInput
        >
            <ul className={style.hours}>
                {[...Array(12).keys()].map((hour) => (
                    <TimeInputUnit
                        key={`time-input-${id}-hour-${hour}`}
                        className={style.hour}
                        selected={hours}
                        onClick={setHour}
                    >
                        {hour}
                    </TimeInputUnit>
                ))}
            </ul>
            <ul className={style.hours}>
                {[...Array(12).keys()]
                    .map((hour) => hour + 12)
                    .map((hour) => (
                        <TimeInputUnit
                            key={`time-input-${id}-hour-${hour}`}
                            className={style.hour}
                            selected={hours}
                            onClick={setHour}
                        >
                            {hour}
                        </TimeInputUnit>
                    ))}
            </ul>
            {precision < 60 && 60 % precision === 0 && (
                <ul
                    className={classNames(style.minutes, style[`precision-${precision}`])}
                >
                    {[...Array(60 / precision).keys()]
                        .map((minute) => minute * precision)
                        .map((minute) => (
                            <TimeInputUnit
                                key={`time-input-${id}-minute-${minute}`}
                                className={style.minute}
                                selected={minutes}
                                precision={precision}
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

export const PrecisionType = PropTypes.oneOf([1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60]);

TimeInput.propTypes = {
    id: PropTypes.string,
    value: PropTypes.arrayOf(PropTypes.number).isRequired,
    onChange: PropTypes.func.isRequired,
    precision: PrecisionType,
};

TimeInput.defaultProps = {
    id: undefined,
    precision: 5,
};

export default TimeInput;
