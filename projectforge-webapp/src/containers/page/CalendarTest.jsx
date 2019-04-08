import React from 'react';
import BigCalendar from 'react-big-calendar';
import moment from 'moment';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop'

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {getServiceURL} from "../../utilities/rest";

moment.locale('de',
    {
        week: {
            dow: 1, // First day of week (got from UserStatus.
            doy: 1, // First day of year (not yet supported).
        }
    })

const localizer = BigCalendar.momentLocalizer(moment) // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar)

class CalendarTestPage extends React.Component {
    state = {
        isFetching: false,
        initalized: false,
        events: []
    };

    constructor(props) {
        super(props);
        this.convertJsonDates = this.convertJsonDates.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
    }

    convertJsonDates = e => Object.assign({}, e, {
        start: new Date(e.start),
        end: new Date(e.end)
    })

    fetchEvents = () => {
        this.setState({
            isFetching: true,
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
                const date = json.date
                const viewType = json.viewType
                const events = json.events
                this.setState({
                    isFetching: false,
                    date: new Date(date),
                    viewType: viewType,
                    events: events.map(this.convertJsonDates),
                    initialized: true
                })
            })
            .catch(() => this.setState({isFetching: false, failed: true}));
    };

    // Callback fired when the date value changes.
    onNavigate = () => {

    }

    // Callback fired when the view value changes.
    onView = () => {

    }

    // Callback fired when the visible date range changes. Returns an Array of dates or an object with start and end dates for BUILTIN views.
    onRangeChange = () => {

    }

    // A callback fired when a date selection is made. Only fires when selectable is true.
    onSelectSlot = () => {

    }

    // Callback fired when a calendar event is selected.
    onSelectEvent = () => {

    }

    // Callback fired when a calendar event is clicked twice.
    onDoubleClickEvent = () => {

    }

    // Callback fired when dragging a selection in the Time views.
    // Returning false from the handler will prevent a selection.
    onSelecting = () => {

    }

    componentDidMount() {
        this.fetchEvents()
    }

    render() {
        if (!this.state.initialized)
            return <React.Fragment>Loading...</React.Fragment>
        return (
            <DragAndDropCalendar
                style={{height: 1000}}
                localizer={localizer}
                events={this.state.events}
                step={30}
                defaultView={this.state.viewType}
                views={['month', 'week', 'day', 'agenda']}
                startAccessor="start"
                defaultDate={this.state.date}
                endAccessor="end"
            />
        );
    }
}

export default CalendarTestPage;
