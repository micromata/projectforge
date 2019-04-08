import React from 'react';
import BigCalendar from 'react-big-calendar';
import moment from 'moment';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop'

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {getServiceURL} from "../../utilities/rest";

moment.locale('de')

const localizer = BigCalendar.momentLocalizer(moment) // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar)

class CalendarTestPage extends React.Component {
    state = {
        isFetching: false,
        events: []
    };

    constructor(props) {
        super(props);
        this.mapToRBCFormat = this.mapToRBCFormat.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
    }

    mapToRBCFormat = e => Object.assign({}, e, {
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
                    date: date,
                    viewType: viewType,
                    events: events.map(this.mapToRBCFormat)
                })
            })
            .catch(() => this.setState({isFetching: false, failed: true}));
    };

    componentDidMount() {
        this.fetchEvents()
    }

    render() {
        console.log(this.state.events)
        return (
            <DragAndDropCalendar
                localizer={localizer}
                events={this.state.events}
                step={30}
                defaultView={this.state.viewTyoe}
                views={['month', 'week', 'day', 'agenda']}
                startAccessor="start"
                defaultDate={this.state.date}
                endAccessor="end"
            />
        );
    }
}

export default CalendarTestPage;
