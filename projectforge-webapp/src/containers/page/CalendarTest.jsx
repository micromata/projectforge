import React, { Component }  from 'react';
import BigCalendar from 'react-big-calendar';
import moment from 'moment';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop'

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';

moment.locale('de')

const localizer = BigCalendar.momentLocalizer(moment) // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar)

const calEvents = []

function CalendarTestPage() {
    return (
        <DragAndDropCalendar
            localizer={localizer}
            events={calEvents}
            step={30}
            defaultView='week'
            views={['month','week','day']}
            startAccessor="start"
            defaultDate={new Date()}
            endAccessor="end"
        />
    );
}

export default CalendarTestPage;
