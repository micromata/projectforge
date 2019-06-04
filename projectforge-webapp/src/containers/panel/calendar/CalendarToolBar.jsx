import React from 'react';
// eslint-disable-next-line camelcase
import moment_timezone from 'moment-timezone';
import { faCaretLeft, faCaretRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import style from '../../../components/design/input/Input.module.scss';


const CalendarToolBar = (toolbar) => {
    const goToBack = () => {
        toolbar.onNavigate('PREV');
    };
    const goToNext = () => {
        toolbar.onNavigate('NEXT');
    };
    const goToToday = () => {
        toolbar.onNavigate('TODAY');
    };

    const goToDayView = () => {
        toolbar.onView('day');
    };
    const goToWeekView = () => {
        toolbar.onView('week');
    };

    const goToWorkWeekView = () => {
        toolbar.onView('work_week');
    };

    const goToMonthView = () => {
        toolbar.onView('month');
    };

    const goToAgendaView = () => {
        toolbar.onView('agenda');
    };

    const { view } = toolbar;

    const label = () => {
        const date = moment_timezone(toolbar.date);
        if (view === 'day') {
            return (
                <React.Fragment>
                    <b>{date.format('dddd')}</b>
                    {' '}
                    {date.format('DD.MM.YYYY')}
                </React.Fragment>
            );
        }
        return (
            <React.Fragment>
                <b>{date.format('MMMM')}</b>
                {' '}
                {date.format('YYYY')}
            </React.Fragment>
        );
    };
    const classNameMonth = (view === 'month') ? 'rbc-active' : '';
    const classNameWeek = (view === 'week') ? 'rbc-active' : '';
    const classNameWorkWeek = (view === 'work_week') ? 'rbc-active' : '';
    const classNameDay = (view === 'day') ? 'rbc-active' : '';
    const classNameAgenda = (view === 'agenda') ? 'rbc-active' : '';
    const { messages } = toolbar.localizer;
    return (
        <div className="rbc-toolbar">
            <span className="rbc-btn-group">
                <button type="button" onClick={goToBack}>
                    <FontAwesomeIcon
                        icon={faCaretLeft}
                        className={style.icon}
                    />
                </button>
                <button type="button" onClick={goToToday}>{messages['calendar.navigation.today']}</button>
                <button
                    type="button"
                    onClick={goToNext}
                >
                    <FontAwesomeIcon
                        icon={faCaretRight}
                        className={style.icon}
                    />
                </button>
            </span>
            <span className="rbc-toolbar-label">{label()}</span>
            <span className="rbc-btn-group">
                <button type="button" className={classNameMonth} onClick={goToMonthView}>{messages['calendar.view.month']}</button>
                <button type="button" className={classNameWeek} onClick={goToWeekView}>{messages['calendar.view.week']}</button>
                <button type="button" className={classNameWorkWeek} onClick={goToWorkWeekView}>{messages['calendar.view.workWeek']}</button>
                <button type="button" className={classNameDay} onClick={goToDayView}>{messages['calendar.view.day']}</button>
                <button
                    type="button"
                    className={classNameAgenda}
                    onClick={goToAgendaView}
                >
                    Agenda
                </button>
            </span>
        </div>
    );
};
export default (CalendarToolBar);
