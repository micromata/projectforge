import React from 'react';
import FullCalendar from '@fullcalendar/react'; // must go before plugins
import deLocale from '@fullcalendar/core/locales/de';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { connect } from 'react-redux'; // a plugin!
import LoadingContainer from '../../../components/design/loading-container';
import { fetchJsonPost } from '../../../utilities/rest';

function FullCalendarPanel(options) {
    const {
        activeCalendars, timesheetUserId, locale, firstDayOfWeek,
        defaultDate, defaultView,
    } = options;
    const [myEvents, setMyEvents] = React.useState({});
    const [loading, setLoading] = React.useState(false);
    const [startDate, setStartDate] = React.useState(defaultDate);
    const [startView, setStartView] = React.useState(defaultView);
    const activeCalendarsRef = React.useRef(activeCalendars);

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

    const eventDidMount = (info) => {
        console.log(info.el, info.event.title, info.event.extendedProps?.category);
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
                timeZone: Intl.DateTimeFormat()
                    .resolvedOptions().timeZone,
            },
            (json) => {
                const { events } = json;
                setMyEvents(myEvents);
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

    return React.useMemo(() => (
        <LoadingContainer loading={loading}>
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
                eventDidMount={eventDidMount}
            />
        </LoadingContainer>
    ), []);
}

const mapStateToProps = ({ authentication }) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekSunday0,
    // timeZone: authentication.user.timeZone,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(FullCalendarPanel);
