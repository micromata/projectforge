import React, { useRef } from 'react';
import FullCalendar from '@fullcalendar/react'; // must go before plugins
import deLocale from '@fullcalendar/core/locales/de';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
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
 - Loading spinner while fetching events.
 - Popovers.
 - AgendaView
 - Grid view for weeks
 - Week view without weekends.
 - save view type after switching.
*/

function FullCalendarPanel(options) {
    const {
        activeCalendars, timesheetUserId, locale, firstDayOfWeek,
        defaultDate, defaultView, match,
    } = options;
    const [myEvents, setMyEvents] = React.useState({});
    const [currentHoverEvent, setCurrentHoverEvent] = React.useState(null);
    const [loading, setLoading] = React.useState(false);
    const activeCalendarsRef = React.useRef(activeCalendars);

    const tooltipRef = useRef(undefined);
    const popperRef = useRef(undefined);

    const calendarRef = React.useRef();

    React.useEffect(() => {
        const api = calendarRef && calendarRef.current && calendarRef.current.getApi();
        if (!api) {
            // console.log('no api yet available.');
            return;
        }
        activeCalendarsRef.current = activeCalendars;
        api.refetchEvents();
    }, [activeCalendars, timesheetUserId]);

    const handleEventMouseEnter = (event) => {
        if (!tooltipRef.current) {
            return;
        }

        if (popperRef.current) {
            popperRef.current.destroy();
        }

        console.log('hover start?');

        setCurrentHoverEvent(event.event);
        popperRef.current = createPopper(event.el, tooltipRef.current, {});
    };

    const handleEventMouseLeave = () => {
        if (popperRef.current) {
            popperRef.current.destroy();
        }
        setCurrentHoverEvent(null);
    };

    const fetchAction = (action, startDate, endDate, allDay, event) => {
        fetchJsonGet(
            'calendar/action',
            {
                action,
                startDate: startDate ? startDate.toISOString() : '',
                endDate: endDate ? endDate.toISOString() : '',
                allDay,
                category: event ? event.category || '' : '',
                dbId: event ? event.dbId || '' : '',
                uid: event ? event.uid || '' : '',
                origStartDate: event && event.start ? event.start.toISOString() : '',
                origEndDate: (event && event.end) ? event.end.toISOString() : '',
                // Browsers time zone may differ from user's time zone:
                // timeZone: Intl.DateTimeFormat()
                //    .resolvedOptions().timeZone,
            },
            (json) => {
                const { url } = json;
                console.log(url, match);
                history.push(`${match.url}${url}`);
            },
        );
    };

    // User wants to create new event (by selecting a time-slot).
    const select = (info) => {
        fetchAction('slotSelected', info.start, info.end);
        // ...
    };

    // User clicked an event.
    const eventClick = (info) => {
        const { event } = info;
        const id = event.extendedProps?.uid || event.extendedProps?.dbId;
        if (id && event.startEditable !== true) return;
        console.log(event.s);
        // start date is send to the server and is needed for series events to detect the
        // current selected event of a series.
        // eslint-disable-next-line max-len
        history.push(`${match.url}/${event.category}/edit/${id}?startDate=${event.start.getTime() / 1000}&endDate=${event.end.getTime() / 1000}`);
    };

    const eventResize = (info) => {
        console.log(info);
        // ...
    };

    const eventDidMount = (info) => {
        // console.log(info.el, info.event.title, info.event.extendedProps?.category);
        /* const tooltip = new Tooltip(info.el, {
            title: info.event.title,
            placement: 'top',
            trigger: 'hover',
            container: 'body',
        }); */
    };

    /*    const eventMouseEnter = (mouseEnterInfo) => {
        const { event } = mouseEnterInfo;
        const popover = (
            <UncontrolledPopover
                target="UncontrolledPopover"
            >
                <PopoverHeader>
                    {event.title}
                </PopoverHeader>
                <PopoverBody>
                    ead Auditor:
                    {' '}
                    <br />
                </PopoverBody>
            </UncontrolledPopover>
        );

        const evtId = `event-${event.id}`;
        const content = (
            <OverlayTrigger placement="bottom" overlay={popover}>
                <div className="fc-content" id={evtId}>
                    <span className="fc-title">{info.event.title}</span>
                </div>
            </OverlayTrigger>
        );

        ReactDOM.render(content, info.el);
    }; */

    const fetchEvents = (info, successCallback, failureCallback) => {
        setLoading(true);
        const { start, end } = info;
        const { current } = activeCalendarsRef;
        const activeCalendarIds = current ? current.map((obj) => obj.id) : [];
        fetchJsonPost(
            'calendar/events',
            {
                start,
                end,
                view: undefined,
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
                setMyEvents(events);
                // Load into FullCalendar
                successCallback(events);
                setLoading(false);
            },
        );
    };

    const views = {
        dayGrid: {
            // options apply to dayGridMonth, dayGridWeek, and dayGridDay views
        },
        timeGrid: {
            // options apply to timeGridWeek and timeGridDay views
        },
        timeGridWeek: {
            // options apply to timeGridWeek and timeGridDay views
        },
        week: {
            // options apply to dayGridWeek and timeGridWeek views
        },
        day: {
            // options apply to dayGridDay and timeGridDay views
        },
    };
    const headerToolbar = { center: 'dayGridMonth,timeGridWeek,timeGridDay,agendaWeek' };
    const locales = [deLocale];

    let initialView;
    switch (defaultView) {
        case 'month':
            initialView = 'dayGridMonth';
            break;
        default:
            initialView = 'timeGridWeek';
    }

    return (
        <LoadingContainer loading={loading}>
            {React.useMemo(() => (
                <FullCalendar
                    plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                    initialView={initialView}
                    events={fetchEvents}
                    editable
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
                    eventMouseEnter={handleEventMouseEnter}
                    eventMouseLeave={handleEventMouseLeave}
                />
            ), [])}
            <CalendarEventTooltip
                forwardRef={tooltipRef}
                event={currentHoverEvent}
                eventClick={eventClick}
                select={select}
                eventResize={eventResize}
                eventDidMount={eventDidMount}
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
