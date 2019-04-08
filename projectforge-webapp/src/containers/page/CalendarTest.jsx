import React, { Component }  from 'react';
import BigCalendar from 'react-big-calendar';
import moment from 'moment';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop'

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {getServiceURL, handleHTTPErrors} from "../../utilities/rest";

moment.locale('de')

const localizer = BigCalendar.momentLocalizer(moment) // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar)

let calEvents = []

class CalendarTestPage extends React.Component {
    constructor(props) {
        super(props);
        fetch(
            getServiceURL('calendar/eventList'),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(json => this.setState({
                loading: false,
                calEvents: json
            }))
            .catch(error => this.setState({
                loading: false,
                error,
            }));
        console.log(calEvents)
    }

    render() {
        return (
            <DragAndDropCalendar
                localizer={localizer}
                events={calEvents}
                step={30}
                defaultView='week'
                views={['month', 'week', 'day']}
                startAccessor="start"
                defaultDate={new Date()}
                endAccessor="end"
            />
        );
    }
}

export default CalendarTestPage;
