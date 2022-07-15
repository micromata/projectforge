import React, { useEffect, useRef, useMemo, useState } from 'react';
import FullCalendar from '@fullcalendar/react'; // must go before plugins
import deLocale from '@fullcalendar/core/locales/de';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import listPlugin from '@fullcalendar/list';
import { connect } from 'react-redux'; // a plugin!
import { createPopper } from '@popperjs/core';
import { Route } from 'react-router-dom';
import LoadingContainer from '../../../components/design/loading-container';
import { fetchJsonPost, fetchJsonGet, fetchGet } from '../../../utilities/rest';
import CalendarEventTooltip from './CalendarEventTooltip';
import history from '../../../utilities/history';
import FormModal from '../../page/form/FormModal';

/*
TODO:
 - Popovers.
 - AgendaView
 - Grid view for weeks
 - Week view without weekends.
 - save view type after switching.
 - Handling of recurring events.
*/

function FullCalendarPanel(options) {
    const {
        activeCalendars, timesheetUserId, locale, firstDayOfWeek,
        defaultDate, defaultView, match, translations,
    } = options;
    const [currentHoverEvent, setCurrentHoverEvent] = useState(null);
    const [loading, setLoading] = useState(false);
    const activeCalendarsRef = useRef(activeCalendars);

    const tooltipRef = useRef(undefined);
    const popperRef = useRef(undefined);

    const calendarRef = useRef();

    useEffect(() => {
        const currentApi = calendarRef && calendarRef.current && calendarRef.current.getApi();
        if (!currentApi) {
            // console.log('no api yet available.');
            return;
        }
        activeCalendarsRef.current = activeCalendars;
        currentApi.refetchEvents();
    }, [activeCalendars, timesheetUserId]);

    const handleEventMouseEnter = (event) => {
        if (!tooltipRef.current) {
            return;
        }

        if (popperRef.current) {
            popperRef.current.destroy();
        }

        setCurrentHoverEvent(event.event);
        popperRef.current = createPopper(event.el, tooltipRef.current, {});
    };

    const handleEventMouseLeave = () => {
        if (popperRef.current) {
            popperRef.current.destroy();
        }
        setCurrentHoverEvent(null);
    };

    // event: event state before resize or move etc.
    const fetchAction = (action, startDate, endDate, allDay, category, event) => {
        const useCategory = category || (event ? event.category || '' : '');
        const dbId = event?.extendedProps?.dbId || '';
        const uid = event?.extendedProps?.uid || '';
        fetchJsonGet(
            'calendar/action',
            {
                action,
                startDate: startDate ? startDate.toISOString() : '',
                endDate: endDate ? endDate.toISOString() : '',
                allDay,
                category: useCategory,
                dbId,
                uid,
                origStartDate: event && event.start ? event.start.toISOString() : '',
                origEndDate: (event && event.end) ? event.end.toISOString() : '',
                // Browsers time zone may differ from user's time zone:
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            },
            (json) => {
                const { url } = json;
                history.push(`${match.url}${url}`);
            },
        );
    };

    // User wants to create new event (by selecting a time-slot).
    const handleSelect = (info) => fetchAction('slotSelected', info.start, info.end);

    // User clicked an event.
    const handleEventClick = (info) => {
        const { event } = info;
        const id = event.extendedProps?.uid || event.extendedProps?.dbId;
        const category = event.extendedProps?.category;
        if (!category || !id || event.startEditable !== true) return;
        // start date is send to the server and is needed for series events to detect the
        // current selected event of a series.
        // eslint-disable-next-line max-len
        history.push(`${match.url}/${category}/edit/${id}?startDate=${event.start.getTime() / 1000}&endDate=${event.end.getTime() / 1000}`);
    };

    const handleEventResize = (info) => {
        info.revert(); // always undo! refetch should handle modified entries.
        const { event, oldEvent } = info;
        const id = event.extendedProps?.uid || event.extendedProps?.dbId;
        const category = event.extendedProps?.category;
        if (!category || !id || event.startEditable !== true) return;
        fetchAction('resize', event.start, event.end, event.allDay, category, oldEvent);
    };

    const handleEventDrop = (info) => {
        info.revert(); // always undo! refetch should handle modified entries.
        const { event, oldEvent } = info;
        const id = event.extendedProps?.uid || event.extendedProps?.dbId;
        const category = event.extendedProps?.category;
        if (!category || !id || event.startEditable !== true) return;
        fetchAction('dragAndDrop', event.start, event.end, event.allDay, category, oldEvent);
    };

    // After changing view type, the view type will be stored on the server in user's pref settings.
    const handleDatesSet = (info) => {
        const view = info?.view?.type;
        if (view) {
            fetchGet(
                'calendar/store',
                {
                    view,
                },
            );
        }
    };

    const fetchEvents = (info, successCallback) => {
        setLoading(true);
        const { start, end } = info;
        const { current } = activeCalendarsRef;
        const activeCalendarIds = current ? current.map((obj) => obj.id) : [];
        fetchJsonPost(
            'calendar/events',
            {
                start,
                end,
                activeCalendarIds,
                timesheetUserId,
                updateState: true,
                useVisibilityState: true,
                // Needed as a workaround if the user's timezone (backend) differs from timezone of
                // the browser. BigCalendar doesn't use moment's timezone for converting the
                // dates start and end. They will be converted by using the browser's timezone.
                // With this timeZone, the server is able to detect the correct start-end
                // interval of the requested events.
                timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            },
            (json) => {
                const { events } = json;
                // Load into FullCalendar
                successCallback(events);
                setLoading(false);
            },
        );
    };

    const views = {
        dayGridWeek: {
            buttonText: `${translations['calendar.view.dayGridWeek']}`,
        },
        timeGridWorkingWeek: {
            type: 'timeGridWeek',
            weekends: false,
            buttonText: `${translations['calendar.view.timeGridWorkingWeek']}`,
        },
        listMonth: {
            buttonText: `${translations['calendar.view.monthAgenda']}`,
        },
        listWeek: {
            buttonText: `${translations['calendar.view.weekAgenda']}`,
        },
    };
    // dayGridMonth=Month, timeGridWeek=Week, timeGridWorkingWeek=Working week,
    // timeGridDay=Day, dayGridWeek=Week list, listWeek=Week agenda, listMonth=Month agenda
    const headerToolbar = { center: 'dayGridMonth,timeGridWeek,timeGridWorkingWeek,timeGridDay,dayGridWeek,listWeek,listMonth' };
    const locales = [deLocale];

    return (
        <LoadingContainer loading={loading}>
            {useMemo(() => (
                <FullCalendar
                    plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
                    initialView={defaultView}
                    initialDate={defaultDate}
                    events={fetchEvents}
                    editable
                    eventResizableFromStart
                    selectable
                    headerToolbar={headerToolbar}
                    allDaySlot
                    views={views}
                    locales={locales}
                    locale={locale}
                    firstDay={firstDayOfWeek}
                    nowIndicator
                    // weekends={false}
                    ref={calendarRef}
                    datesSet={handleDatesSet}
                    eventClick={handleEventClick}
                    select={handleSelect}
                    eventResize={handleEventResize}
                    eventDrop={handleEventDrop}
                    eventMouseEnter={handleEventMouseEnter}
                    eventMouseLeave={handleEventMouseLeave}
                />
            ), [])}
            <CalendarEventTooltip
                forwardRef={tooltipRef}
                event={currentHoverEvent}
                eventClick={handleEventClick}
                select={handleSelect}
                eventResize={handleEventResize}
                eventMouseEnter={handleEventMouseEnter}
                eventMouseLeave={handleEventMouseLeave}
            />
            <Route
                path={`${match.url}/:category/:type/:id?`}
                render={(props) => <FormModal baseUrl={match.url} {...props} />}
            />
        </LoadingContainer>
    );
}

const mapStateToProps = ({ authentication }) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekSunday0,
    // timeZone: authentication.user.timeZone,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(FullCalendarPanel);
