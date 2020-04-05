import PropTypes from 'prop-types';
import React from 'react';
import BookLendOut from './components/BookLendOut';
import CalendarEditExternalSubscription from './components/CalendarEditExternalSubscription';
import CalendarEventRecurrency from './components/CalendarEventRecurrence';
import CalendarEventReminder from './components/CalendarEventReminder';
import CalendarSubscriptionInfo from './components/CalendarSubscriptionInfo';
import CustomizedAddressImage from './components/CustomizedAddressImage';
import CustomizedAddressPhoneNumbers from './components/CustomizedAddressPhoneNumbers';
import CustomizedConsumptionBar from './components/CustomizedConsumptionBar';
import DayRange from './components/DayRange';
import CustomizedImageDataPreview from './components/ImageDataPreview';
import JiraIssuesLinks from './components/JiraIssuesLinks';
import TimesheetEditTaskAndKost2 from './components/timesheet/TimesheetEditTaskAndKost2';
import TimesheetTemplatesAndRecent from './components/timesheet/TimesheetTemplatesAndRecent';
import VacationStatistics from './components/vacation/VacationStatistics';
import VacationTable from './components/vacation/VacationTable';

function DynamicCustomized({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'address.edit.image':
            Tag = CustomizedAddressImage;
            break;
        case 'address.imagePreview':
            Tag = CustomizedImageDataPreview;
            break;
        case 'address.phoneNumbers':
            Tag = CustomizedAddressPhoneNumbers;
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
        case 'calendar.editExternalSubscription':
            Tag = CalendarEditExternalSubscription;
            break;
        case 'calendar.subscriptionInfo':
            Tag = CalendarSubscriptionInfo;
            break;
        case 'dayRange':
            Tag = DayRange;
            break;
        case 'jira.issuesLinks':
            Tag = JiraIssuesLinks;
            break;
        case 'task.consumption':
            Tag = CustomizedConsumptionBar;
            break;
        case 'timesheet.edit.taskAndKost2':
            Tag = TimesheetEditTaskAndKost2;
            break;
        case 'timesheet.edit.templatesAndRecents':
            Tag = TimesheetTemplatesAndRecent;
            break;
        case 'vacation.entries':
            Tag = VacationTable;
            break;
        case 'vacation.statistics':
            Tag = VacationStatistics;
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
