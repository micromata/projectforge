import moment_timezone from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import BigCalendar from 'react-big-calendar';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop';

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {connect} from 'react-redux';
import {getServiceURL} from '../../utilities/rest';
import CalendarToolBar from './CalendarToolBar';

import 'moment/min/locales';
import history from "../../utilities/history";

const localizer = BigCalendar.momentLocalizer(moment_timezone); // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar);

class CalendarPage extends React.Component {
    state = {
        initalized: false,
        events: [],
        specialDays: [],
        date: undefined,
        viewType: undefined
    };

    // ToDo
    // DateHeader for statistics.

    renderEvent = ({event}) => {
        let location = undefined;
        let desc = undefined;
        let formattedDuration = undefined;
        if (event.location) location = <React.Fragment>{event.location}<br/></React.Fragment>;
        if (event.desc) desc = <React.Fragment>{event.description}<br/></React.Fragment>;
        if (event.formattedDuration) formattedDuration =
            <React.Fragment>{event.formattedDuration}<br/></React.Fragment>;
        return (
            <React.Fragment>
                <p><strong>{event.title}</strong></p>
                {location}{desc}{formattedDuration}
            </React.Fragment>
        )
    };

    renderMonthEvent = ({event}) => {
        return (<React.Fragment>{event.title}</React.Fragment>);
    };

    renderAgendaEvent = ({event}) => {
        return (
            <React.Fragment>
                {event.title}
            </React.Fragment>
        )
    };

    eventStyle = (event) => {
        if (this.state.viewType === 'agenda')
            return { // Don't change style for agenda:
                className: ''
            };
        // Event is always undefined!!!
        const backgroundColor = (event && event.bgColor) ? event.bgColor : undefined;
        const textColor = (event && event.fgColor) ? event.fgColor : undefined;
        const cssClass = (event && event.cssClass) ? event.cssClass : undefined;
        return {
            style: {
                backgroundColor: backgroundColor,
                color: textColor
            },
            className: cssClass
        };
    };
    renderDateHeader = (obj) => {
        const isoDate = moment_timezone(obj.date).format('YYYY-MM-DD');
        const specialDay = this.state.specialDays[isoDate];
        let dayInfo = '';
        if (specialDay && specialDay.holidayTitle) {
            dayInfo = `${specialDay.holidayTitle} `;
        }
        return <React.Fragment><a href={'#'} onClick={() => this.navigateToDay(obj.date)}>{dayInfo}{obj.label}</a></React.Fragment>;
    }

    dayStyle = (date) => {
        const isoDate = moment_timezone(date).format('YYYY-MM-DD');
        const specialDay = this.state.specialDays[isoDate];
        if (!specialDay)
            return;
        let className = 'holiday';
        if (specialDay.workingDay)
            className = 'holiday-workday';
        else if (specialDay.weekend) {
            if (specialDay.holiday)
                className = 'weekend-holiday';
            else
                className = 'weekend';
        } else
            className = 'holiday';
        return {
            className: className
        }
    };

    navigateToDay = (e) => {
        this.setState({
            date: e,
            viewType: 'day'
        })
    }

    convertJsonDates = e => Object.assign({}, e, {
        start: new Date(e.start),
        end: new Date(e.end)
    });

    fetchInitial = () => {
        this.setState({
            failed: false
        });
        fetch(getServiceURL('calendar/initial'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => response.json())
            .then(json => {
                const date = json.date;
                const viewType = json.viewType;
                const events = json.events;
                const specialDays = json.specialDays;
                this.setState({
                    date: new Date(date),
                    viewType: viewType,
                    events: events.map(this.convertJsonDates),
                    specialDays: specialDays,
                    initialized: true
                })
            })
            .catch(() => this.setState({initialized: false, failed: true}));
    };

    fetchEvents = (start, end, view) => {
        this.setState({
            failed: false
        });
        fetch(getServiceURL('calendar/events', {
            start: start ? start.toJSON() : '',
            end: end ? end.toJSON() : '',
            view: view ? view : 'month'
        }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => response.json())
            .then(json => {
                const events = json.events;
                const specialDays = json.specialDays;
                this.setState({
                    events: events.map(this.convertJsonDates),
                    specialDays: specialDays,
                })
            })
            .catch(() => this.setState({failed: true}));
    };

    onNavigate = (date) => {
        this.setState({date: date})
    };

    onView = (obj) => {
    };

    // Callback fired when the visible date range changes. Returns an Array of dates or an object with start and end dates for BUILTIN views.
    onRangeChange = (event, view) => {
        let viewType = view;
        if (view) {
            this.setState({viewType: view});
        } else
            viewType = this.state.viewType;
        let start;
        let end;
        if (viewType === 'month' || viewType === 'agenda') {
            start = event.start;
            end = event.end;
        } else {
            start = event[0];
        }
        // console.log("start:", start, "end", end, viewType)
        this.fetchEvents(start, end, viewType);
    };

    // A callback fired when a date selection is made. Only fires when selectable is true.
    onSelectSlot = () => {

    };

    // Callback fired when a calendar event is selected.
    onSelectEvent = (event) => {
        history.push(event.link);
    };

    // Callback fired when a calendar event is clicked twice.
    onDoubleClickEvent = () => {

    };

    // Callback fired when dragging a selection in the Time views.
    // Returning false from the handler will prevent a selection.
    onSelecting = () => {

    };

    componentDidMount() {
        this.fetchInitial()
    };

    render() {
        if (!this.state.initialized)
            return <React.Fragment>Loading...</React.Fragment>;
        let initTime = new Date(this.state.date.getDate());
        initTime.setHours( 8);
        initTime.setMinutes(0);
        return (
            <DragAndDropCalendar
                style={{
                    minHeight: 800,
                    height: 'calc(100vh - 164px)',
                }}
                localizer={localizer}
                events={this.state.events}
                step={30}
                defaultView={this.state.viewType}
                view={this.state.viewType}
                onView={this.onView}
                views={['month', 'work_week', 'week', 'day', 'agenda']}
                startAccessor="start"
                date={this.state.date}
                onNavigate={this.onNavigate}
                endAccessor="end"
                onRangeChange={this.onRangeChange}
                onSelectEvent={this.onSelectEvent}
                eventPropGetter={this.eventStyle}
                dayPropGetter={this.dayStyle}
                showMultiDayTimes={true}
                timeslots={1}
                scrollToTime={initTime}
                components={{
                    event: this.renderEvent,
                    month: {
                        event: this.renderMonthEvent,
                        dateHeader: this.renderDateHeader
                    },
                    week: {
                        //header: this.renderDateHeader
                    },
                    agenda: {
                        event: this.renderAgendaEvent,
                    },
                    toolbar : CalendarToolBar
                }}
            />
        );
    }

    constructor(props) {
        super(props);

        const {firstDayOfWeek, timeZone, locale} = this.props;
        const useLocale = (locale) ? locale : 'en'
        moment_timezone.tz.setDefault(timeZone);
        moment_timezone.locale(useLocale,
            {
                week: {
                    dow: firstDayOfWeek, // First day of week (got from UserStatus).
                    doy: 1, // First day of year (not yet supported).
                }
            });

        this.convertJsonDates = this.convertJsonDates.bind(this);
        this.renderEvent = this.renderEvent.bind(this);
        this.renderMonthEvent = this.renderMonthEvent.bind(this);
        this.renderAgendaEvent = this.renderAgendaEvent.bind(this);
        this.renderDateHeader = this.renderDateHeader.bind(this);
        this.eventStyle = this.eventStyle.bind(this);
        this.dayStyle = this.dayStyle.bind(this);
        this.navigateToDay = this.navigateToDay.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
        this.fetchInitial = this.fetchInitial.bind(this);
        this.onRangeChange = this.onRangeChange.bind(this);
        this.onSelectSlot = this.onSelectSlot.bind(this);
        this.onSelectEvent = this.onSelectEvent.bind(this);
        this.onDoubleClickEvent = this.onDoubleClickEvent.bind(this);
        this.onSelecting = this.onSelecting.bind(this);
        this.onNavigate = this.onNavigate.bind(this);
        this.onView = this.onView.bind(this);
    }
}

CalendarPage.defaultProps = {
    firstDayOfWeek: PropTypes.number.isRequired,
    timeZone: PropTypes.number.isRequired,
    locale: PropTypes.String
};

const mapStateToProps = ({authentication}) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
    timeZone: authentication.user.timeZone,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(CalendarPage);
