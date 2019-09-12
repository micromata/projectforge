import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import DayPicker from 'react-day-picker';
import { connect } from 'react-redux';
import { Col, Row } from '../..';
import style from './CalendarInput.module.scss';

function DateTimeRange(
    {
        firstDayOfWeek,
        from,
        jsDateFormat,
        jsTimestampFormatMinutes,
        onChange,
        selectors,
        to,
        ...props
    },
) {
    const handleDayClick = (day) => {
        const newRange = {
            from,
            to,
        };

        if (from === undefined || day < from) {
            newRange.from = day;
            if (from) {
                newRange.from.setHours(from.getHours());
                newRange.from.setMinutes(from.getMinutes());
            } else {
                newRange.from.setHours(0);
            }
        } else {
            newRange.to = day;
            if (to) {
                newRange.to.setHours(to.getHours());
                newRange.to.setMinutes(to.getMinutes());
            } else {
                newRange.to.setHours(23);
                newRange.to.setMinutes(59);
            }
        }

        onChange(newRange);
    };

    const handleWeekClick = (weekNumber, days) => {
        const newRange = {
            from: days[0],
            to: days[days.length - 1],
        };

        newRange.from.setHours(0);
        newRange.from.setMinutes(0);
        newRange.to.setHours(23);
        newRange.to.setMinutes(59);

        onChange(newRange);
    };

    const handleMonthClick = (firstDayOfMonth) => {
        if (!selectors.includes('MONTH')) {
            return;
        }

        onChange({
            from: firstDayOfMonth,
            to: moment(firstDayOfMonth)
                .endOf('month')
                .toDate(),
        });
    };

    return (
        <Row>
            {selectors.includes('UNTIL_NOW') && (
                <Col sm={3}>
                    <ul className={style.quickSelectors}>
                        <li className={style.quickSelector}>[Letzte 30 Minuten]</li>
                        <li className={style.quickSelector}>[Letzte Stunde]</li>
                        <li className={style.quickSelector}>[Heute]</li>
                        <li className={style.quickSelector}>[Seit gestern]</li>
                        <li className={style.quickSelector}>[Letzte Woche]</li>
                        <li className={style.quickSelector}>[Letzte 2 Wochen]</li>
                        <li className={style.quickSelector}>[Letzten Monat]</li>
                        <li className={style.quickSelector}>[Letzte 3 Monate]</li>
                    </ul>
                </Col>
            )}
            <Col sm={9}>
                <p className={style.label}>
                    {!from && !to && '[Bitte wähle das Startdatum aus]'}
                    {from && !to && '[Bitte wähle das Enddatum aus]'}
                    {from && to && (
                        <React.Fragment>
                            {`${moment(from)
                                .format(from.getHours() === 0 && from.getMinutes() === 0 ? jsDateFormat : jsTimestampFormatMinutes)} - ${moment(to)
                                .format(to.getHours() === 23 && to.getMinutes() === 59 ? jsDateFormat : jsTimestampFormatMinutes)} `}
                            <FontAwesomeIcon
                                icon={faTimes}
                                onClick={() => onChange({})}
                            />
                        </React.Fragment>
                    )}
                </p>
                <DayPicker
                    className="range"
                    firstDayOfWeek={firstDayOfWeek}
                    fromMonth={from}
                    modifiers={{
                        start: from,
                        end: to,
                    }}
                    showWeekNumbers={selectors.includes('WEEK')}
                    month={from}
                    numberOfMonths={2}
                    onCaptionClick={handleMonthClick}
                    onDayClick={handleDayClick}
                    onWeekClick={handleWeekClick}
                    selectedDays={[from, {
                        from,
                        to,
                    }]}
                    {...props}
                />
            </Col>
        </Row>
    );
}

DateTimeRange.propTypes = {
    onChange: PropTypes.func.isRequired,
    jsDateFormat: PropTypes.string.isRequired,
    jsTimestampFormatMinutes: PropTypes.string.isRequired,
    firstDayOfWeek: PropTypes.number,
    from: PropTypes.instanceOf(Date),
    locale: PropTypes.string,
    selectors: PropTypes.arrayOf(PropTypes.string),
    timeNotation: PropTypes.string,
    to: PropTypes.instanceOf(Date),
};

DateTimeRange.defaultProps = {
    firstDayOfWeek: 1,
    from: undefined,
    locale: 'en',
    selectors: undefined,
    timeNotation: 'H24',
    to: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
    jsDateFormat: authentication.user.jsDateFormatShort,
    jsTimestampFormatMinutes: authentication.user.jsTimestampFormatMinutes,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DateTimeRange);
