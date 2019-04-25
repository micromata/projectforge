import React from 'react';
import timezone from 'moment-timezone';

export const renderEvent = (event) => {
    let location;
    let desc;
    let formattedDuration;
    if (event.location) {
        location = (
            <React.Fragment>
                {event.location}
                <br />
            </React.Fragment>
        );
    }
    if (event.desc) {
        desc = (
            <React.Fragment>
                {event.description}
                <br />
            </React.Fragment>
        );
    }
    if (event.formattedDuration) {
        formattedDuration = (
            <React.Fragment>
                {event.formattedDuration}
                <br />
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
};

export const renderMonthEvent = event => <React.Fragment>{event.title}</React.Fragment>;

export const renderAgendaEvent = event => <React.Fragment>{event.title}</React.Fragment>;

export const renderDateHeader = (entry, specialDays, navigateToDay) => {
    const isoDate = timezone(entry.date)
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
                onClick={() => navigateToDay(entry.date)}
            >
                {dayInfo}
                {entry.label}
            </div>
        </React.Fragment>
    );
};

export const dayStyle = (date, specialDays) => {
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
};
