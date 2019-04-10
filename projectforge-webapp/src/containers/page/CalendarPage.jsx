import moment_timezone from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import BigCalendar from 'react-big-calendar';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop';

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {connect} from 'react-redux';
import {getServiceURL} from '../../utilities/rest';

const localizer = BigCalendar.momentLocalizer(moment_timezone); // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar);

class CalendarPage extends React.Component {
    state = {
        initalized: false,
        events: []
    };

    // ToDo
    // DateHeader for statistics.

    renderEvent = ({event}) => {
        let location = undefined;
        let desc = undefined;
        let formattedDuration = undefined;
        if (event.location) location = <React.Fragment>{event.location}<br/></React.Fragment>;
        if (event.desc) desc = <React.Fragment>{event.description}<br/></React.Fragment>;
        if (event.formattedDuration) formattedDuration = <React.Fragment>{event.formattedDuration}<br/></React.Fragment>;
        return (
            <React.Fragment>
                <p><strong>{event.title}</strong></p>
                {location}{desc}{formattedDuration}
            </React.Fragment>
        )
    };

    renderMonthEvent = ({event}) => {
        return (
            <React.Fragment>
                {event.title}
            </React.Fragment>
        )
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
        const backgroundColor = (event && event.bgColor) ? event.bgColor : '#3174ad';
        const textColor = (event && event.fgColor) ? event.fgColor : 'black';
        const style = {
            backgroundColor: backgroundColor,
            color: textColor,
            borderRadius: '3px',
            opacity: 0.8,
            border: '0px',
            display: 'block'
        };
        return {
            className: '',
            style: style
        };
    };

    dayStyle = (date) => {
        return {
            className: ''
        }
    };

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
                this.setState({
                    date: new Date(date),
                    viewType: viewType,
                    events: events.map(this.convertJsonDates),
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
                this.setState({
                    events: events.map(this.convertJsonDates),
                })
            })
            .catch(() => this.setState({failed: true}));
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
    onSelectEvent = () => {

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
                views={['month', 'week', 'day', 'agenda']}
                startAccessor="start"
                defaultDate={this.state.date}
                endAccessor="end"
                onRangeChange={this.onRangeChange}
                eventPropGetter={this.eventStyle}
                dayPropGetter={this.dayStyle}
                components={{
                    event: this.renderEvent,
                    month: {
                        event: this.renderMonthEvent,
                    },
                    agenda: {
                        event: this.renderAgendaEvent,
                    },
                }}
            />
        );
    }

    constructor(props) {
        super(props);

        const {firstDayOfWeek, timeZone} = this.props;
console.log(timeZone)
        moment_timezone.tz.setDefault(timeZone);
        moment_timezone.locale('de',
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
        this.eventStyle = this.eventStyle.bind(this);
        this.dayStyle = this.dayStyle.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
        this.fetchInitial = this.fetchInitial.bind(this);
        this.onRangeChange = this.onRangeChange.bind(this);
        this.onSelectSlot = this.onSelectSlot.bind(this);
        this.onSelectEvent = this.onSelectEvent.bind(this);
        this.onDoubleClickEvent = this.onDoubleClickEvent.bind(this);
        this.onSelecting = this.onSelecting.bind(this);
    }
}

CalendarPage.defaultProps = {
    firstDayOfWeek: PropTypes.number.isRequired,
    timeZone: PropTypes.number.isRequired
};

const mapStateToProps = ({authentication}) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
    timeZone: authentication.user.timeZone
});

export default connect(mapStateToProps)(CalendarPage);
