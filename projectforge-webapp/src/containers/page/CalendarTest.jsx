import moment from 'moment';
import PropTypes from 'prop-types';
import React from 'react';
import BigCalendar from 'react-big-calendar';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop';

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {connect} from 'react-redux';
import {getServiceURL} from '../../utilities/rest';

const localizer = BigCalendar.momentLocalizer(moment); // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar);

class CalendarTestPage extends React.Component {
    state = {
        initalized: false,
        events: []
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
            view: view ? view : "month"
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
        console.log("start:", start, "end", end, viewType)
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
            return <React.Fragment>Loading...</React.Fragment>
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
            />
        );
    }

    constructor(props) {
        super(props);

        const {firstDayOfWeek} = this.props;

        moment.locale('de',
            {
                week: {
                    dow: firstDayOfWeek, // First day of week (got from UserStatus).
                    doy: 1, // First day of year (not yet supported).
                }
            });

        this.convertJsonDates = this.convertJsonDates.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
        this.fetchInitial = this.fetchInitial.bind(this);
        this.onRangeChange = this.onRangeChange.bind(this);
        this.onSelectSlot = this.onSelectSlot.bind(this);
        this.onSelectEvent = this.onSelectEvent.bind(this);
        this.onDoubleClickEvent = this.onDoubleClickEvent.bind(this);
        this.onSelecting = this.onSelecting.bind(this);
    }
}

CalendarTestPage.defaultProps = {
    firstDayOfWeek: PropTypes.number.isRequired,
};

const mapStateToProps = ({authentication}) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
});

export default connect(mapStateToProps)(CalendarTestPage);
