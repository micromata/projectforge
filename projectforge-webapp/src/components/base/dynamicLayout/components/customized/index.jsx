import PropTypes from 'prop-types';
import React from 'react';
import CalendarEventRecurrency from './components/CalendarEventRecurrence';
import CustomizedColorChooser from './components/CustomizedColorChooser';
import CustomizedConsumptionBar from './components/CustomizedConsumptionBar';
import CustomizedEMail from './components/CustomizedEMail';
import DayRange from './components/DayRange';
import JiraIssuesLinks from './components/JiraIssuesLinks';
import TimesheetEditTaskAndKost2 from './components/timesheet/TimesheetEditTaskAndKost2';
import TimesheetTemplatesAndRecent from './components/timesheet/TimesheetTemplatesAndRecent';
import CalendarEventReminder from './components/CalendarEventReminder';
import CalendarEditExternalSubscription from './components/CalendarEditExternalSubscription';
import CalendarSubscriptionInfo from './components/CalendarSubscriptionInfo';
import CostNumberComponent from './components/CostNumberComponent';
import AccessTableComponent from './components/AccessTableComponent';
import InvoicePositionsComponent from './components/OutgoingInvoicePositionsComponent';
import IncomingInvoicePositionsComponent from './components/IncomingInvoicePositionsComponent';

function DynamicCustomized({ id, ...props }) {
    let Tag;

    switch (id) {
        case 'access.table':
            Tag = AccessTableComponent;
            break;
        case 'email':
            Tag = CustomizedEMail;
            break;
        case 'color-chooser':
            Tag = CustomizedColorChooser;
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
        case 'timesheet.edit.templatesAndRecent':
            Tag = TimesheetTemplatesAndRecent;
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

export default DynamicCustomized;
