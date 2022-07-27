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
import { fetchJsonPost, fetchJsonGet } from '../../../utilities/rest';
import CalendarEventTooltip from './CalendarEventTooltip';
import history from '../../../utilities/history';
import FormModal from '../../page/form/FormModal';

/*
TODO:
 - Aktuelles Datum und Ansicht werden überbügelt nach initialCall.
 - org.projectforge.business.address.AddressDao.hasAccess (AddressDao.java:291): session is null.
 - Handling of recurring events.
 - Unterscheidung: freie Feiertage und andere Feiertage.
 - Update Kalender und ggf. Datum nach Anlage/Editieren von Einträgen
 - height of calendar
 - Breaks
*/

function FullCalendarPanel(options) {
    const {
        activeCalendars, timesheetUserId, locale, firstDayOfWeek,
        defaultDate, defaultView, match, translations, gridSize,
        vacationGroups, vacationUsers,
    } = options;
    const queryParams = new URLSearchParams(window.location.search);
    const hash = queryParams.get('hash');
    const [currentHoverEvent, setCurrentHoverEvent] = useState(null);
    const [loading, setLoading] = useState(false);
    const [currentState, setCurrentState] = useState({
        date: defaultDate,
        view: defaultView,
    });
    const activeCalendarsRef = useRef(activeCalendars);
    const timesheetUserIdRef = useRef(timesheetUserId);

    const tooltipRef = useRef(undefined);
    const popperRef = useRef(undefined);

    const calendarRef = useRef();

    // Needed as a workaround if the user's timezone (backend) differs from timezone of
    // the browser. BigCalendar doesn't use moment's timezone for converting the
    // dates start and end. They will be converted by using the browser's timezone.
    // With this timeZone, the server is able to detect the correct start-end
    // interval of the requested events.
    const { timeZone } = Intl.DateTimeFormat().resolvedOptions();

    const getSlotDuration = (newGridSize) => {
        let slotDuration = `00:${newGridSize}:00`;
        if (newGridSize < 10) {
            slotDuration = `00:0${newGridSize}:00`;
        }
        return slotDuration;
    };

    useEffect(() => {
        const currentApi = calendarRef && calendarRef.current && calendarRef.current.getApi();
        if (!currentApi) {
            // console.log('no api yet available.');
            return;
        }
        activeCalendarsRef.current = activeCalendars;
        timesheetUserIdRef.current = timesheetUserId;
        console.log('refetch', activeCalendars, timesheetUserId, vacationGroups, vacationUsers, hash, currentState);
        currentApi.refetchEvents();
    }, [activeCalendars, timesheetUserId, vacationGroups, vacationUsers, hash]);

    useEffect(() => {
        const currentApi = calendarRef && calendarRef.current && calendarRef.current.getApi();
        if (!currentApi) {
            // console.log('no api yet available.');
            return;
        }
        currentApi.slotDuration = getSlotDuration(gridSize);
    }, [gridSize]);

    useEffect(() => {
        // Current state (view/date) changed, so store it in the user's prefs:
        const { date, view } = currentState;
        console.log('storeState', date, view, activeCalendars);
        if (date && view) {
            fetchJsonPost(
                'calendar/storeState',
                {
                    date: date.toISOString(),
                    view,
                    timeZone,
                    activeCalendars,
                },
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                () => { },
            );
        }
    }, [activeCalendars, currentState.date, currentState.view]);

    const handleEventMouseEnter = (info) => {
        if (!tooltipRef.current) {
            return;
        }

        if (popperRef.current) {
            popperRef.current.destroy();
        }

        setCurrentHoverEvent(info.event);
        const tooltip = info.event?.extendedProps?.tooltip;
        if (tooltip) {
            popperRef.current = createPopper(info.el, tooltipRef.current, { });
        }
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
                timeZone,
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
        if (category === 'address') {
            history.push(`${match.url}/addressView/dynamic/${id}`);
        } else {
            history.push(`${match.url}/${category}/edit/${id}?startDate=${event.start.getTime() / 1000}&endDate=${event.end.getTime() / 1000}`);
        }
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
        const start = info?.start;
        console.log('handleDatesSet', info);
        if (view && start) {
            setCurrentState({
                date: start,
                view,
            });
        }
    };

    const fetchEvents = (info, successCallback) => {
        setLoading(true);
        const { start, end } = info;
        const { current: activeCalendarsCurrent } = activeCalendarsRef;
        const activeCalendarIds = activeCalendarsCurrent
            ? activeCalendarsCurrent.map((obj) => obj.id) : [];
        const { current: timesheetUserIdCurrent } = timesheetUserIdRef;
        fetchJsonPost(
            'calendar/events',
            {
                start,
                end,
                activeCalendarIds,
                timesheetUserId: timesheetUserIdCurrent,
                useVisibilityState: true,
                // Needed as a workaround if the user's timezone (backend) differs from timezone of
                // the browser. BigCalendar doesn't use moment's timezone for converting the
                // dates start and end. They will be converted by using the browser's timezone.
                // With this timeZone, the server is able to detect the correct start-end
                // interval of the requested events.
                timeZone,
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
            slotDuration: `${getSlotDuration(gridSize)}`,
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
            ), [gridSize])}
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
