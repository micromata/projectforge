import React from 'react';
import moment_timezone from 'moment-timezone';

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

    const label = () => {
        const date = moment_timezone(toolbar.date);
        if (view === 'day')
            return <span><b>{date.format('dddd')}</b><span> {date.format('DD.MM.YYYY')}</span></span>
        return (
            <span><b>{date.format('MMMM')}</b><span> {date.format('YYYY')}</span></span>
        );
    };
    const view = toolbar.view;
    const classNameMonth = (view === 'month') ? 'rbc-active' : '';
    const classNameWeek = (view === 'week') ? 'rbc-active' : '';
    const classNameWorkWeek = (view === 'work_week') ? 'rbc-active' : '';
    const classNameDay = (view === 'day') ? 'rbc-active' : '';
    const classNameAgenda = (view === 'agenda') ? 'rbc-active' : '';
    const messages = toolbar.localizer.messages;
    return (
        <div className="rbc-toolbar">
            <span className="rbc-btn-group">
                <button type="button" onClick={goToBack}>{messages.previous}</button>
                <button type="button" onClick={goToToday}>{messages.today}</button>
                <button type="button" onClick={goToNext}>{messages.next}</button></span>
            <span className="rbc-toolbar-label">{label()}</span>
            <span className="rbc-btn-group">
                <button type="button" className={classNameMonth} onClick={goToMonthView}>Month</button>
                <button type="button" className={classNameWeek} onClick={goToWeekView}>Week</button>
                <button type="button" className={classNameWorkWeek} onClick={goToWorkWeekView}>Work Week</button>
                <button type="button" className={classNameDay} onClick={goToDayView}>Day</button>
                <button type="button" className={classNameAgenda} onClick={goToAgendaView}>Agenda</button>
            </span>
        </div>
    );
};
export default (CalendarToolBar);
