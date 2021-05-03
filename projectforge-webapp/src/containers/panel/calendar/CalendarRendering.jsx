import React from 'react';
import timezone from 'moment-timezone';

export const renderEvent = (event) => {
    let location;
    let desc;
    let formattedDuration;
    if (event.location) {
        location = (
            <>
                {event.location}
                <br />
            </>
        );
    }
    if (event.desc) {
        desc = (
            <>
                {event.description}
                <br />
            </>
        );
    }
    if (event.formattedDuration) {
        formattedDuration = (
            <>
                {event.formattedDuration}
                <br />
            </>
        );
    }
    return (
        <>
            <p><strong>{event.title}</strong></p>
            {location}
            {desc}
            {formattedDuration}
        </>
    );
};

export const renderMonthEvent = (event) => <>{event.title}</>;

export const renderAgendaEvent = (event) => <>{event.title}</>;

export const renderDateHeader = (entry, specialDays, navigateToDay) => {
    const isoDate = timezone(entry.date)
        .format('YYYY-MM-DD');
    const specialDay = specialDays[isoDate];
    let dayInfo = '';
    if (specialDay && specialDay.holidayTitle) {
        dayInfo = `${specialDay.holidayTitle} `;
    }
    return (
        <>
            <div
                role="presentation"
                onClick={() => navigateToDay(entry.date)}
            >
                {dayInfo}
                {entry.label}
            </div>
        </>
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
