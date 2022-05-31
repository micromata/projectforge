import React from 'react';
// eslint-disable-next-line camelcase
import moment_timezone from 'moment-timezone';
import { faCaretLeft, faCaretRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import style from '../../../components/design/input/Input.module.scss';

function CalendarToolBar(toolbar) {
    const goToBack = () => {
        const { onNavigate } = toolbar;
        onNavigate('PREV');
    };
    const goToNext = () => {
        const { onNavigate } = toolbar;
        onNavigate('NEXT');
    };
    const goToToday = () => {
        const { onNavigate } = toolbar;
        onNavigate('TODAY');
    };

    const goToDayView = () => {
        const { onView } = toolbar;
        onView('day');
    };
    const goToWeekView = () => {
        const { onView } = toolbar;
        onView('week');
    };

    const goToWorkWeekView = () => {
        const { onView } = toolbar;
        onView('work_week');
    };

    const goToMonthView = () => {
        const { onView } = toolbar;
        onView('month');
    };

    const goToAgendaView = () => {
        const { onView } = toolbar;
        onView('agenda');
    };

    const { view } = toolbar;

    const label = () => {
        const { date } = toolbar;
        const momentDate = moment_timezone(date);
        if (view === 'day') {
            return (
                <>
                    <b>{momentDate.format('dddd')}</b>
                    {' '}
                    {momentDate.format('DD.MM.YYYY')}
                </>
            );
        }
        return (
            <>
                <b>{momentDate.format('MMMM')}</b>
                {' '}
                {momentDate.format('YYYY')}
            </>
        );
    };
    const classNameMonth = (view === 'month') ? 'rbc-active' : '';
    const classNameWeek = (view === 'week') ? 'rbc-active' : '';
    const classNameWorkWeek = (view === 'work_week') ? 'rbc-active' : '';
    const classNameDay = (view === 'day') ? 'rbc-active' : '';
    const classNameAgenda = (view === 'agenda') ? 'rbc-active' : '';
    const { localizer } = toolbar;
    const { messages } = localizer;
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
}
export default (CalendarToolBar);
