import React from 'react';
import momentTimezone from 'moment-timezone';
import { faCaretLeft, faCaretRight } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import style from '../../../components/design/input/Input.module.scss';

function CalendarToolBar(toolbar) {
    const { onNavigate, onView, date: toolbarDate } = toolbar;

    const goToBack = () => {
        onNavigate('PREV');
    };
    const goToNext = () => {
        onNavigate('NEXT');
    };
    const goToToday = () => {
        onNavigate('TODAY');
    };

    const goToDayView = () => {
        onView('day');
    };
    const goToWeekView = () => {
        onView('week');
    };

    const goToWorkWeekView = () => {
        onView('work_week');
    };

    const goToMonthView = () => {
        onView('month');
    };

    const goToAgendaView = () => {
        onView('agenda');
    };

    const { view } = toolbar;

    const label = () => {
        const date = momentTimezone(toolbarDate);
        if (view === 'day') {
            return (
                <>
                    <b>{date.format('dddd')}</b>
                    {' '}
                    {date.format('DD.MM.YYYY')}
                </>
            );
        }
        return (
            <>
                <b>{date.format('MMMM')}</b>
                {' '}
                {date.format('YYYY')}
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
