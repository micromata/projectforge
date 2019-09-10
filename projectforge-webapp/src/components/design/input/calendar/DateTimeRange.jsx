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
        from,
        jsDateFormat,
        onChange,
        selectors,
        to,
        ...props
    },
) {
    console.log(jsDateFormat);

    const handleDayClick = (day) => {
        const newRange = {
            from,
            to,
        };

        if (from === undefined || day < from) {
            newRange.from = day;
        } else {
            newRange.to = day;
        }

        onChange(newRange);
    };

    return (
        <Row>
            <Col sm={12}>
                <p className={style.label}>
                    {!from && !to && '[Bitte wähle das Startdatum aus]'}
                    {from && !to && '[Bitte wähle das Enddatum aus]'}
                    {from && to && (
                        <React.Fragment>
                            {`${moment(from)
                                .format(jsDateFormat)} - ${moment(to)
                                .format(jsDateFormat)} `}
                            <FontAwesomeIcon
                                icon={faTimes}
                                onClick={() => onChange({})}
                            />
                        </React.Fragment>
                    )}
                </p>
            </Col>
            {selectors.includes('UNTIL_NOW') ? (
                <Col sm={3}>
                    <ul className={style.quickSelectors}>
                        <li className={style.quickSelector}>[Letzte Minute]</li>
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
            ) : undefined}
            <Col sm={9}>
                <DayPicker
                    className="Range"
                    onDayClick={handleDayClick}
                    numberOfMonths={2}
                    selectedDays={[from, {
                        from,
                        to,
                    }]}
                    modifiers={{
                        start: from,
                        end: to,
                    }}
                    month={from}
                    fromMonth={from}
                    {...props}
                />
            </Col>
        </Row>
    );
}

DateTimeRange.propTypes = {
    onChange: PropTypes.func.isRequired,
    from: PropTypes.instanceOf(Date),
    jsDateFormat: PropTypes.string.isRequired,
    locale: PropTypes.string,
    selectors: PropTypes.arrayOf(PropTypes.string),
    timeNotation: PropTypes.string,
    to: PropTypes.instanceOf(Date),
};

DateTimeRange.defaultProps = {
    from: undefined,
    locale: 'en',
    selectors: undefined,
    timeNotation: 'H24',
    to: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    jsDateFormat: authentication.user.jsDateFormat,
    locale: authentication.user.locale,
    timeNotation: authentication.user.timeNotation,
});

export default connect(mapStateToProps)(DateTimeRange);
