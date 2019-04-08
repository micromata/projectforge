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
        initalized: false,
        events: []
    };

    convertJsonDates = e => Object.assign({}, e, {
        start: new Date(e.start),
        end: new Date(e.end)
    })

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
                const date = json.date
                const viewType = json.viewType
                const events = json.events
                this.setState({
                    date: new Date(date),
                    viewType: viewType,
                    events: events.map(this.convertJsonDates),
                    initialized: true
                })
            })
            .catch(() => this.setState({initialized: false, failed: true}));
    };

    fetchEvents = () => {
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
                const date = json.date
                const viewType = json.viewType
                const events = json.events
                this.setState({
                    date: new Date(date),
                    viewType: viewType,
                    events: events.map(this.convertJsonDates),
                    initialized: true
                })
            })
            .catch(() => this.setState({failed: true}));
    };

    // Callback fired when the date value changes.
    onNavigate = event => {
        console.log("onNavigate")
        console.log(event)
    }

    // Callback fired when the view value changes.
    onView = event => {
        console.log("onView")
        console.log(event)
    }

    // Callback fired when the visible date range changes. Returns an Array of dates or an object with start and end dates for BUILTIN views.
    onRangeChange = event => {
        const start = event.start;
        const end = event.end;
        console.log("onRangeChange")
        console.log(event)
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
        this.fetchInitial()
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
                onNavigate={this.onNavigate}
                onView={this.onView}
                onRangeChange={this.onRangeChange}
            />
        );
    }

    constructor(props) {
        super(props);
        this.convertJsonDates = this.convertJsonDates.bind(this);
        //this.fetchEvents = this.fetchEvents.bind(this);
        this.fetchInitial = this.fetchInitial.bind(this);
        this.onNavigate = this.onNavigate.bind(this);
        this.onView = this.onView.bind(this);
        this.onRangeChange = this.onRangeChange.bind(this);
        this.onSelectSlot = this.onSelectSlot.bind(this);
        this.onSelectEvent = this.onSelectEvent.bind(this);
        this.onDoubleClickEvent = this.onDoubleClickEvent.bind(this);
        this.onSelecting = this.onSelecting.bind(this);
    }
}

export default CalendarTestPage;
