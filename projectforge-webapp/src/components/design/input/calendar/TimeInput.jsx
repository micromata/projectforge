import classNames from 'classnames';
import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import AdvancedPopper from '../../popper/AdvancedPopper';
import style from './CalendarInput.module.scss';
import TimeInputUnit from './TimeInputUnit';

const hourRegex = /^([01]?[0-9]|2[0-3]|)$/;
const minuteRegex = /^([0-5]?[0-9]|)$/;


function TimeInput(
    {
        id,
        jsDateFormat,
        precision,
        setTime,
        showDate,
        time,
    },
) {
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
        if (!minuteRegex.test(minute)) {
            return;
        }

        if (minute < 0 || minute > 59) {
            return;
        }

        const newTime = new Date(time);
        newTime.setMinutes(minute);

        setTime(newTime);
    };

    const handleDateBlur = ({ target }) => {
        const newDate = moment(target.value, jsDateFormat);

        if (!newDate.isValid()) {
            return;
        }

        // Manipulating with MomentJS
        newDate.hour(time.getHours());
        newDate.minute(time.getMinutes());

        setTime(newDate.toDate());
    };

    const handleHourChange = ({ target }) => {
        if (!hourRegex.test(target.value)) {
            return;
        }

        setHour(Number(target.value));
    };

    const handleMinuteChange = ({ target }) => {
        if (!minuteRegex.test(target.value)) {
            return;
        }

        setMinute(Number(target.value));
    };

    return (
        <React.Fragment>
            {showDate && (
                <input
                    size={jsDateFormat.length + 1}
                    onBlur={handleDateBlur}
                    defaultValue={moment(time)
                        .format(jsDateFormat)}
                />
            )}
            <AdvancedPopper
                basic={(
                    <div onClick={() => setIsOpen(true)} role="presentation">
                        <input
                            ref={hourRef}
                            type="number"
                            min={0}
                            max={23}
                            value={time.getHours()}
                            className={style.hourInput}
                            onChange={handleHourChange}
                        />
                        <span>:</span>
                        <input
                            ref={minuteRef}
                            type="number"
                            min={0}
                            max={59}
                            step={precision}
                            className={style.minuteInput}
                            onChange={handleMinuteChange}
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
                                    precision={precision}
                                    onClick={setMinute}
                                >
                                    {minute}
                                </TimeInputUnit>
                            ))}
                    </ul>
                )}
            </AdvancedPopper>
        </React.Fragment>
    );
}

TimeInput.propTypes = {
    id: PropTypes.string.isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    setTime: PropTypes.func.isRequired,
    time: PropTypes.instanceOf(Date).isRequired,
    precision: PropTypes.oneOf([1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60]),
    showDate: PropTypes.bool,
};

TimeInput.defaultProps = {
    precision: 5,
    showDate: false,
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
});

export default connect(mapStateToProps)(TimeInput);
