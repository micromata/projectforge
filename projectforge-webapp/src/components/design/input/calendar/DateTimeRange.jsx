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
        to,
        ...props
    },
) {
    const [quickSelector, setQuickSelector] = React.useState(undefined);

    const handleQuickSelectorClick = interval => () => {
        setQuickSelector(interval);

        const newFrom = new Date();

        switch (interval) {
            case 'LAST_30_MINUTES':
                newFrom.setMinutes(newFrom.getMinutes() - 30);
                break;
            case 'LAST_HOUR':
                newFrom.setHours(newFrom.getHours() - 1);
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
            case 'LAST_WEEK':
                newFrom.setDate(newFrom.getDate() - 7);
                break;
            case 'LAST_2_WEEKS':
                newFrom.setDate(newFrom.getDate() - 14);
                break;
            case 'LAST_MONTH':
                newFrom.setDate(newFrom.getDate() - 31);
                break;
            case 'LAST_3_MONTHS':
                newFrom.setDate(newFrom.getDate() - 92);
                break;
            case 'YEAR':
                newFrom.setMinutes(0);
                newFrom.setHours(0);
                newFrom.setDate(1);
                newFrom.setMonth(0);
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
            label: '[Letzte 30 Minuten]',
            id: 'LAST_30_MINUTES',
        },
        {
            label: '[Letzte Stunde]',
            id: 'LAST_HOUR',
        },
        {
            label: '[Heute]',
            id: 'TODAY',
        },
        {
            label: '[Seit gestern]',
            id: 'SINCE_YESTERDAY',
        },
        {
            label: '[Letzte Woche]',
            id: 'LAST_WEEK',
        },
        {
            label: '[Letzte 2 Wochen]',
            id: 'LAST_2_WEEKS',
        },
        {
            label: '[Letzten Monat]',
            id: 'LAST_MONTH',
        },
        {
            label: '[Letzte 3 Monate]',
            id: 'LAST_3_MONTHS',
        },
        {
            label: '[Dieses Jahr]',
            id: 'YEAR',
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
                <p className={style.label}>
                    {!from && !to && '[Bitte wähle das Startdatum aus]'}
                    {from && !to && '[Bitte wähle das Enddatum aus]'}
                    {from && to && (
                        <FormattedTimeRange to={to} from={from}>
                            {' '}
                            <FontAwesomeIcon
                                icon={faTimes}
                                onClick={() => onChange({})}
                            />
                        </FormattedTimeRange>
                    )}
                </p>
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
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DateTimeRange);
