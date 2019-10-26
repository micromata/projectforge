import PropTypes from 'prop-types';
import React from 'react';
import BookLendOut from './components/BookLendOut';
import CalendarEventRecurrency from './components/CalendarEventRecurrence';
import CustomizedAddressImage from './components/CustomizedAddressImage';
import CustomizedConsumptionBar from './components/CustomizedConsumptionBar';
import CustomizedImageDataPreview from './components/ImageDataPreview';
import DayRange from './components/DayRange';
import TimesheetEditTaskAndKost2 from './components/timesheet/TimesheetEditTaskAndKost2';
import TimesheetTemplatesAndRecents from './components/timesheet/TimesheetTemplatesAndRecents';
import CalendarEventReminder from './components/CalendarEventReminder';

function DynamicCustomized({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'address.edit.image':
            Tag = CustomizedAddressImage;
            break;
        case 'address.imagePreview':
            Tag = CustomizedImageDataPreview;
            break;
        case 'book.lendOutComponent':
            Tag = BookLendOut;
            break;
        case 'calendar.recurrency':
            Tag = CalendarEventRecurrency;
            break;
        case 'calendar.reminder':
            Tag = CalendarEventReminder;
            break;
        case 'dayRange':
            Tag = DayRange;
            break;
        case 'task.consumption':
            Tag = CustomizedConsumptionBar;
            break;
        case 'timesheet.edit.taskAndKost2':
            Tag = TimesheetEditTaskAndKost2;
            break;
        case 'timesheet.edit.templatesAndRecents':
            Tag = TimesheetTemplatesAndRecents;
            break;
        default:
            return <span>{`Customized field '${id}' not found!`}</span>;
    }

    return (
        <Tag id={id} {...props} />
    );
}

DynamicCustomized.propTypes = {
    id: PropTypes.string.isRequired,
};

DynamicCustomized.defaultProps = {};

export default DynamicCustomized;
