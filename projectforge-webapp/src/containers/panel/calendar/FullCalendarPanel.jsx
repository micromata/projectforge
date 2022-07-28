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
 - Handling of recurring events.
 - Breaks
*/

function FullCalendarPanel(options) {
    const {
        activeCalendars, timesheetUserId, locale, firstDayOfWeek,
        defaultDate, defaultView, match, translations, gridSize,
        vacationGroups, vacationUsers, topHeight,
    } = options;
    const [queryString, setQueryString] = useState(window.location.search);
    const [gotoDate, setGotoDate] = useState(new URLSearchParams(window.location.search).get('gotoDate'));
    const [currentHoverEvent, setCurrentHoverEvent] = useState(null);
    const [loading, setLoading] = useState(false);
    let initialDate = defaultDate;
    if (defaultDate && defaultView === 'dayGridMonth' && !initialDate.endsWith('01')) {
        // Fullcalendar needs day of month to view as start date, not start-date of begin of
        // first week with day of last month:
        let date = new Date(initialDate);
        date = new Date(date.getFullYear(), date.getMonth() + 1, 1);
        initialDate = Date.toIsoDateString(date);
    }
    const [currentState, setCurrentState] = useState({
        date: initialDate,
        view: defaultView,
    });
    const activeCalendarsRef = useRef(activeCalendars);
    const timesheetUserIdRef = useRef(timesheetUserId);

    const tooltipRef = useRef(undefined);
    const popperRef = useRef(undefined);

    const calendarRef = useRef();

    useEffect(() => {
        // onComponentDidMount
    }, []);

    const refetch = (currentApi) => {
        if (!currentApi) {
            // console.log('currentApi not yet available');
            return;
        }
        activeCalendarsRef.current = activeCalendars;
        timesheetUserIdRef.current = timesheetUserId;
        currentApi.refetchEvents();
    };

    const firstUpdate = useRef(true);

    useEffect(() => {
        if (firstUpdate.current) {
            firstUpdate.current = false;
            return;
        }
        const currentApi = calendarRef && calendarRef.current && calendarRef.current.getApi();
        // eslint-disable-next-line max-len
        // console.log('refetch', activeCalendars, timesheetUserId, vacationGroups, vacationUsers);
        refetch(currentApi);
    }, [activeCalendars, timesheetUserId, vacationGroups, vacationUsers]);

    useEffect(() => {
        const currentApi = calendarRef && calendarRef.current && calendarRef.current.getApi();
        if (!currentApi) {
            // console.log('currentApi not yet available');
            return;
        }
        if (queryString !== window.location.search) {
            const newQueryParams = new URLSearchParams(window.location.search);
            const queryParams = new URLSearchParams(queryString);
            const dateChanged = newQueryParams.get('gotoDate') !== undefined && queryParams.get('gotoDate') !== newQueryParams.get('gotoDate');
            const hashChanged = newQueryParams.get('hash') !== undefined && queryParams.get('hash') !== newQueryParams.get('hash');
            if (!dateChanged && !hashChanged) {
                return;
            }
            // Hash param or date changed:
            // console.log('queryString', queryString, window.location.search);
            const date = newQueryParams.get('gotoDate');
            let refetchTriggerd = false;
            if (date) {
                const viewStart = Date.toIsoDateString(currentApi.view.activeStart);
                const viewEnd = Date.toIsoDateString(currentApi.view.activeEnd);
                // console.log(date, viewStart, viewEnd, date < viewStart);
                if (date < viewStart || date > viewEnd) {
                    // console.log('gotoDate', date);
                    currentApi.gotoDate(date);
                    refetchTriggerd = true;
                }
            }
            if (!refetchTriggerd && hashChanged) {
                // console.log('hashChanged');
                refetch(currentApi);
            }
        }
    }, [window.location.search]);

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
        // console.log('gridSize', gridSize, getSlotDuration(gridSize));
        currentApi.slotDuration = getSlotDuration(gridSize);
    }, [gridSize]);

    useEffect(() => {
        // Current state (view/date) changed, so store it in the user's prefs:
        const { date, view } = currentState;
        if (date && view) {
            // console.log('storeState', date, view);
            fetchJsonPost(
                'calendar/storeState',
                {
                    date,
                    view,
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
        // console.log('handleDatesSet', info);
        const view = info?.view?.type;
        const startDate = info?.start;
        if (view && startDate) {
            const start = Date.toIsoDateString(startDate);
            // console.log('handleDatesSet', start);
            setCurrentState({
                date: start,
                view,
            });
        }
    };

    const fetchEvents = (info, successCallback) => {
        // console.log('fetchEvents', info);
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
        dayGridMonth: {
            fixedWeekCount: false,
        },
        dayGridWeek: {
            buttonText: `${translations['calendar.view.dayGridWeek']}`,
        },
        timeGridWeek: {
            slotDuration: `${getSlotDuration(gridSize)}`,
            scrollTime: '08:00:00',
        },
        timeGridWorkingWeek: {
            type: 'timeGridWeek',
            weekends: false,
            buttonText: `${translations['calendar.view.timeGridWorkingWeek']}`,
            slotDuration: `${getSlotDuration(gridSize)}`,
            scrollTime: '08:00:00',
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

    // console.log('FullCalendarPanel.render', defaultDate, defaultView);
    return (
        <LoadingContainer loading={loading}>
            {useMemo(() => (
                <FullCalendar
                    plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
                    initialView={defaultView}
                    initialDate={initialDate}
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
                    ref={calendarRef}
                    datesSet={handleDatesSet}
                    eventClick={handleEventClick}
                    select={handleSelect}
                    eventResize={handleEventResize}
                    eventDrop={handleEventDrop}
                    eventMouseEnter={handleEventMouseEnter}
                    eventMouseLeave={handleEventMouseLeave}
                    height={`calc(100vh - ${topHeight})`}
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
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(FullCalendarPanel);
