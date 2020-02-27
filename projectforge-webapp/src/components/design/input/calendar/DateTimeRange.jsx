import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import DayPicker from 'react-day-picker';
import { connect } from 'react-redux';
import { Col, Row } from '../..';
import style from './CalendarInput.module.scss';
import FormattedTimeRange from './FormattedTimeRange';

function DateTimeRange(
    {
        firstDayOfWeek,
        from,
        id,
        onChange,
        selectors,
        setFrom,
        setTo,
        to,
        ...props
    },
) {
    const [quickSelector, setQuickSelector] = React.useState(undefined);

    const handleQuickSelectorClick = interval => () => {
        setQuickSelector(interval);

        const newFrom = new Date();

        switch (interval) {
            case 'LAST_MINUTE':
                newFrom.setMinutes(newFrom.getMinutes() - 1);
                break;
            case 'LAST_MINUTES_10':
                newFrom.setMinutes(newFrom.getMinutes() - 10);
                break;
            case 'LAST_MINUTES_30':
                newFrom.setMinutes(newFrom.getMinutes() - 30);
                break;
            case 'LAST_HOUR':
                newFrom.setHours(newFrom.getHours() - 1);
                break;
            case 'LAST_HOURS_4':
                newFrom.setHours(newFrom.getHours() - 4);
                break;
            case 'TODAY':
                newFrom.setHours(0);
                newFrom.setMinutes(0);
                break;
            case 'SINCE_YESTERDAY':
                newFrom.setHours(0);
                newFrom.setMinutes(0);
                newFrom.setDate(newFrom.getDate() - 1);
                break;
            case 'LAST_DAYS_3':
                newFrom.setDate(newFrom.getDate() - 3);
                break;
            case 'LAST_DAYS_7':
                newFrom.setDate(newFrom.getDate() - 7);
                break;
            case 'LAST_DAYS_30':
                newFrom.setDate(newFrom.getDate() - 30);
                break;
            case 'LAST_DAYS_90':
                newFrom.setDate(newFrom.getDate() - 90);
                break;
            default:
        }

        onChange({
            from: newFrom,
            to: new Date(),
        });
    };

    const handleDayClick = (day) => {
        setQuickSelector();

        const newRange = {
            from,
            to,
        };

        if (!from || day < from) {
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
        setQuickSelector();

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

        setQuickSelector();

        onChange({
            from: firstDayOfMonth,
            to: moment(firstDayOfMonth)
                .endOf('month')
                .toDate(),
        });
    };

    const quickSelectors = [
        {
            label: 'search.lastMinutes',
            id: 'LAST_MINUTE',
        },
        {
            label: 'search.lastMinutes.10',
            id: 'LAST_MINUTES_10',
        },
        {
            label: 'search.lastMinutes.30',
            id: 'LAST_MINUTES_30',
        },
        {
            label: 'search.lastHour',
            id: 'LAST_HOUR',
        },
        {
            label: 'search.lastHours.4',
            id: 'LAST_HOURS_4',
        },
        {
            label: 'today',
            id: 'TODAY',
        },
        {
            label: 'search.sinceYesterday',
            id: 'SINCE_YESTERDAY',
        },
        {
            label: 'search.lastDays.3',
            id: 'LAST_DAYS_3',
        },
        {
            label: 'search.lastDays.7',
            id: 'LAST_DAYS_7',
        },
        {
            label: 'search.lastDays.30',
            id: 'LAST_DAYS_30',
        },
        {
            label: 'search.lastDays.90',
            id: 'LAST_DAYS_90',
        },
    ];

    return (
        <Row>
            {selectors.includes('UNTIL_NOW') && (
                <Col sm={3}>
                    <ul className={style.quickSelectors}>
                        {quickSelectors.map(selector => (
                            <li
                                className={classNames(
                                    style.quickSelector,
                                    { [style.selected]: selector.id === quickSelector },
                                )}
                                key={`quick-selector-${id}-${selector.id}`}
                                onClick={handleQuickSelectorClick(selector.id)}
                                role="presentation"
                            >
                                {selector.label}
                            </li>
                        ))}
                    </ul>
                </Col>
            )}
            <Col sm={9}>
                <span className={style.label}>
                    {!from && !to && '[date.begin]'}
                    {from && !to && '[date.end]'}
                    {from && to && (
                        <FormattedTimeRange
                            from={from}
                            id={`date-time-range-${id}`}
                            setFrom={setFrom}
                            setTo={setTo}
                            to={to}
                        >
                            {' '}
                            <FontAwesomeIcon
                                icon={faTimes}
                                onClick={() => onChange({})}
                            />
                        </FormattedTimeRange>
                    )}
                </span>
                <DayPicker
                    className="range"
                    firstDayOfWeek={firstDayOfWeek}
                    modifiers={{
                        start: from,
                        end: to,
                    }}
                    showWeekNumbers={selectors.includes('WEEK')}
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
    id: PropTypes.string.isRequired,
    firstDayOfWeek: PropTypes.number,
    from: PropTypes.instanceOf(Date),
    locale: PropTypes.string,
    setFrom: PropTypes.func,
    setTo: PropTypes.func,
    selectors: PropTypes.arrayOf(PropTypes.string),
    timeNotation: PropTypes.string,
    to: PropTypes.instanceOf(Date),
};

DateTimeRange.defaultProps = {
    firstDayOfWeek: 1,
    from: undefined,
    locale: 'en',
    setFrom: undefined,
    setTo: undefined,
    selectors: undefined,
    timeNotation: 'H24',
    to: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DateTimeRange);
