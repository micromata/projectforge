import timezone from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import BigCalendar from 'react-big-calendar';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop';

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { connect } from 'react-redux';
import { getServiceURL } from '../../utilities/rest';
import CalendarToolBar from './CalendarToolBar';

import 'moment/min/locales';
import history from '../../utilities/history';
import LoadingContainer from '../../components/design/loading-container';

const localizer = BigCalendar.momentLocalizer(timezone); // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar);

class CalendarPanel extends React.Component {
    static renderEvent(event) {
        let location;
        let desc;
        let formattedDuration;
        if (event.location) {
            location = (
                <React.Fragment>
                    {event.location}
                    <br/>
                </React.Fragment>
            );
        }
        if (event.desc) {
            desc = (
                <React.Fragment>
                    {event.description}
                    <br/>
                </React.Fragment>
            );
        }
        if (event.formattedDuration) {
            formattedDuration = (
                <React.Fragment>
                    {event.formattedDuration}
                    <br/>
                </React.Fragment>
            );
        }
        return (
            <React.Fragment>
                <p><strong>{event.title}</strong></p>
                {location}
                {desc}
                {formattedDuration}
            </React.Fragment>
        );
    }

    static renderMonthEvent(event) {
        return (<React.Fragment>{event.title}</React.Fragment>);
    }

    static renderAgendaEvent(event) {
        return (
            <React.Fragment>
                {event.title}
            </React.Fragment>
        );
    }

    // Callback fired when a calendar event is selected.
    static onSelectEvent(event) {
        history.push(event.link);
    }

    constructor(props) {
        super(props);

        const { firstDayOfWeek, timeZone, locale } = this.props;
        const useLocale = locale || 'en';
        timezone.tz.setDefault(timeZone);
        timezone.updateLocale(useLocale,
            {
                week: {
                    dow: firstDayOfWeek, // First day of week (got from UserStatus).
                    doy: 1, // First day of year (not yet supported).
                },
            });

        const { defaultDate, defaultView } = props;

        this.state = {
            loading: false,
            events: undefined,
            specialDays: undefined,
            date: defaultDate,
            view: defaultView,
            start: defaultDate,
            end: undefined,
            calendar: '',
        };

        this.renderDateHeader = this.renderDateHeader.bind(this);
        this.eventStyle = this.eventStyle.bind(this);
        this.dayStyle = this.dayStyle.bind(this);
        this.navigateToDay = this.navigateToDay.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
        this.onRangeChange = this.onRangeChange.bind(this);
        this.onSelectSlot = this.onSelectSlot.bind(this);
        this.onDoubleClickEvent = this.onDoubleClickEvent.bind(this);
        this.onSelecting = this.onSelecting.bind(this);
        this.onNavigate = this.onNavigate.bind(this);
        this.onView = this.onView.bind(this);
        this.convertJsonDates = this.convertJsonDates.bind(this);
    }

    componentDidMount() {
        this.fetchEvents();
    }

    convertJsonDates = e => Object.assign({}, e, {
        start: new Date(e.start),
        end: new Date(e.end),
    });


    // ToDo
    // DateHeader for statistics.

    onNavigate(date) {
        this.setState({ date });
    }

    onView(obj) {
    }

    // Callback fired when the visible date range changes. Returns an Array of dates or an object
    // with start and end dates for BUILTIN views.
    onRangeChange(event, newView) {
        const { view } = this.state;
        let useView = newView;
        if (newView) {
            this.setState({ view: newView });
        } else {
            // newView isn't given (view not changed), so get view from state:
            useView = view;
        }
        const { start, end } = event;
        let newStart;
        let newEnd;
        if (useView === 'month' || useView === 'agenda') {
            newStart = start;
            newEnd = end;
        } else {
            const [element] = event;
            newStart = element;
        }
        this.setState({
            start: newStart,
            end: newEnd,
            view: useView
        }, () => this.fetchEvents());
        // console.log("start:", myStart, "end", myEnd, useView)
    }

    // A callback fired when a date selection is made. Only fires when selectable is true.
    onSelectSlot(slotInfo) {
        const { calendar } = this.state;
        fetch(getServiceURL('calendar/action', {
            action: 'select',
            start: slotInfo.start ? slotInfo.start.toJSON() : '',
            end: slotInfo.end ? slotInfo.end.toJSON() : '',
            calendar,
        }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const redirectUrl = json.url;
                history.push(redirectUrl);
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    // Callback fired when a calendar event is clicked twice.
    onDoubleClickEvent() {

    }

    // Callback fired when dragging a selection in the Time views.
    // Returning false from the handler will prevent a selection.
    onSelecting(event) {
        console.log('onSelecting', event);
    }

    eventStyle(event) {
        const { viewType } = this.state;
        if (viewType === 'agenda') {
            return { // Don't change style for agenda:
                className: '',
            };
        }
        // Event is always undefined!!!
        const backgroundColor = (event && event.bgColor) ? event.bgColor : undefined;
        const textColor = (event && event.fgColor) ? event.fgColor : undefined;
        const cssClass = (event && event.cssClass) ? event.cssClass : undefined;
        return {
            style: {
                backgroundColor,
                color: textColor,
            },
            className: cssClass,
        };
    }

    dayStyle(date) {
        const { specialDays } = this.state;
        const isoDate = timezone(date)
            .format('YYYY-MM-DD');
        const specialDay = specialDays[isoDate];
        if (!specialDay) {
            return '';
        }
        let className = 'holiday';
        if (specialDay.workingDay) {
            className = 'holiday-workday';
        } else if (specialDay.weekend) {
            if (specialDay.holiday) {
                className = 'weekend-holiday';
            } else {
                className = 'weekend';
            }
        } else {
            className = 'holiday';
        }
        return { className };
    }

    navigateToDay(e) {
        this.setState({
            date: e,
            viewType: 'day',
        });
    }

    fetchEvents() {
        const { start, end, view } = this.state;
        const { activeCalendars } = this.props;
        const activeCalendarIds = activeCalendars ? activeCalendars.map(obj => obj.id) : [];
        this.setState({ loading: true });
        fetch(getServiceURL('calendar/events'), {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                start: start ? start.toJSON() : null,
                end: end ? end.toJSON() : null,
                view: view,
                activeCalendarIds,
            }),
        })
            .then(response => response.json())
            .then((json) => {
                const { events, specialDays } = json;
                this.setState({
                    loading: false,
                    events: events.map(this.convertJsonDates),
                    specialDays,
                });
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    renderDateHeader(obj) {
        const { specialDays } = this.state;
        const isoDate = timezone(obj.date)
            .format('YYYY-MM-DD');
        const specialDay = specialDays[isoDate];
        let dayInfo = '';
        if (specialDay && specialDay.holidayTitle) {
            dayInfo = `${specialDay.holidayTitle} `;
        }
        return (
            <React.Fragment>
                <div
                    role="presentation"
                    onClick={() => this.navigateToDay(obj.date)}
                >
                    {dayInfo}
                    {obj.label}
                </div>
            </React.Fragment>
        );
    }

    render() {
        const { events, loading } = this.state;
        if (!events) {
            return (
                <LoadingContainer loading={loading}>
                    ...
                </LoadingContainer>
            );
        }
        const { date, view } = this.state;
        const { topHeight } = this.props;
        let initTime = new Date(date.getDate());
        initTime.setHours(8);
        initTime.setMinutes(0);
        return (
            <LoadingContainer loading={loading}>
                <DragAndDropCalendar
                    style={{
                        minHeight: 500,
                        height: `calc(100vh - ${topHeight})`,
                    }}
                    localizer={localizer}
                    events={events}
                    step={30}
                    view={view}
                    onView={this.onView}
                    views={['month', 'work_week', 'week', 'day', 'agenda']}
                    startAccessor="start"
                    date={this.state.date}
                    onNavigate={this.onNavigate}
                    endAccessor="end"
                    onRangeChange={this.onRangeChange}
                    onSelectEvent={CalendarPanel.onSelectEvent}
                    onSelectSlot={this.onSelectSlot}
                    selectable
                    eventPropGetter={this.eventStyle}
                    dayPropGetter={this.dayStyle}
                    showMultiDayTimes
                    timeslots={1}
                    scrollToTime={initTime}
                    components={{
                        event: CalendarPanel.renderEvent,
                        month: {
                            event: CalendarPanel.renderMonthEvent,
                            dateHeader: CalendarPanel.renderDateHeader,
                        },
                        week: {
                            //header: CalendarPanel.renderDateHeader
                        },
                        agenda: {
                            event: CalendarPanel.renderAgendaEvent,
                        },
                        toolbar: CalendarToolBar,
                    }}
                />
            </LoadingContainer>
        );
    }
}

CalendarPanel.propTypes = {
    activeCalendars: PropTypes.arrayOf(PropTypes.shape({})),
    firstDayOfWeek: PropTypes.number.isRequired,
    timeZone: PropTypes.string.isRequired,
    locale: PropTypes.string,
    topHeight: PropTypes.string,
    defautlDate: PropTypes.instanceOf(Date),
    defaultView: PropTypes.string,
};

CalendarPanel.defaultProps = {
    activeCalendars: [],
    locale: undefined,
    topHeight: '164px',
    defaultDate: new Date(),
    defaultView: 'month',
};

const mapStateToProps = ({ authentication }) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
    timeZone: authentication.user.timeZone,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(CalendarPanel);
