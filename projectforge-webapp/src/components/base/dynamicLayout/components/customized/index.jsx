import PropTypes from 'prop-types';
import React from 'react';
import BookLendOut from './components/BookLendOut';
import CalendarEventRecurrency from './components/CalendarEventRecurrence';
import CustomizedAddressImage from './components/CustomizedAddressImage';
import CustomizedAddressPhoneNumbers from './components/CustomizedAddressPhoneNumbers';
import CustomizedConsumptionBar from './components/CustomizedConsumptionBar';
import CustomizedImageDataPreview from './components/ImageDataPreview';
import DayRange from './components/DayRange';
import JiraIssuesLinks from './components/JiraIssuesLinks';
import TimesheetEditTaskAndKost2 from './components/timesheet/TimesheetEditTaskAndKost2';
import TimesheetTemplatesAndRecents from './components/timesheet/TimesheetTemplatesAndRecents';
import CalendarEventReminder from './components/CalendarEventReminder';
import CalendarEditExternalSubscription from './components/CalendarEditExternalSubscription';
import CalendarSubscriptionInfo from './components/CalendarSubscriptionInfo';
import VacationEntries from './components/VacationEntries';
import VacationStatistics from './components/VacationStatistics';
import CostNumberComponent from "./components/CostNumberComponent";
import AccessTableComponent from "./components/AccessTableComponent";
import InvoicePositionsComponent from "./components/OutgoingInvoicePositionsComponent";
import IncomingInvoicePositionsComponent from "./components/IncomingInvoicePositionsComponent";

function DynamicCustomized({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'access.table':
            Tag = AccessTableComponent;
            break;
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
        case 'cost.number':
            Tag = CostNumberComponent;
            break;
        case 'dayRange':
            Tag = DayRange;
            break;
        case 'invoice.incomingPosition':
            Tag = IncomingInvoicePositionsComponent;
            break;
        case 'invoice.outgoingPosition':
            Tag = InvoicePositionsComponent;
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
            Tag = TimesheetTemplatesAndRecents;
            break;
        case 'vacation.entries':
            Tag = VacationEntries;
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
