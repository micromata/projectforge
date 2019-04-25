import timezone from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { Modal, ModalBody } from 'reactstrap';
import BigCalendar from 'react-big-calendar';
import withDragAndDrop from 'react-big-calendar/lib/addons/dragAndDrop';

import 'react-big-calendar/lib/addons/dragAndDrop/styles.css';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { connect } from 'react-redux';
import { getServiceURL } from '../../utilities/rest';
import CalendarToolBar from './CalendarToolBar';

import 'moment/min/locales';
import LoadingContainer from '../../components/design/loading-container';
import TimesheetEditPanel from './TimesheetEditPanel';

/* eslint-disable-next-line object-curly-newline */
import { renderEvent, renderMonthEvent, renderAgendaEvent, renderDateHeader, dayStyle } from './CalendarRendering';

const localizer = BigCalendar.momentLocalizer(timezone); // or globalizeLocalizer

const DragAndDropCalendar = withDragAndDrop(BigCalendar);

class CalendarPanel extends React.Component {
    constructor(props) {
        super(props);

        const { firstDayOfWeek, timeZone, locale } = this.props;
        const useLocale = locale || 'en';
        timezone.tz.setDefault(timeZone);
        timezone.updateLocale(useLocale,
            {
                week: {
                    dow: firstDayOfWeek, // First day of week (got from UserStatus).
                    doy: 1, // First day of year (not yet supported).
                },
            });

        const { defaultDate, defaultView } = props;

        this.state = {
            loading: false,
            events: undefined,
            specialDays: undefined,
            date: defaultDate,
            view: defaultView,
            start: defaultDate,
            end: undefined,
            calendar: '',
            editPanel: {
                visible: false,
                category: undefined,
                dbId: undefined,
                startDate: undefined,
                endDate: undefined,
            },
        };

        this.eventStyle = this.eventStyle.bind(this);
        this.navigateToDay = this.navigateToDay.bind(this);
        this.fetchEvents = this.fetchEvents.bind(this);
        this.onRangeChange = this.onRangeChange.bind(this);
        this.onSelectSlot = this.onSelectSlot.bind(this);
        this.onSelectEvent = this.onSelectEvent.bind(this);
        this.onDoubleClickEvent = this.onDoubleClickEvent.bind(this);
        this.onSelecting = this.onSelecting.bind(this);
        this.onNavigate = this.onNavigate.bind(this);
        this.onView = this.onView.bind(this);
        this.convertJsonDates = this.convertJsonDates.bind(this);
        this.toggleEditModal = this.toggleEditModal.bind(this);
    }

    componentDidMount() {
        this.fetchEvents();
    }

    componentDidUpdate(prevProps) {
        const { activeCalendars } = this.props;
        if (prevProps.activeCalendars.length !== activeCalendars.length) {
            this.fetchEvents();
        }
    }

    // ToDo
    // DateHeader for statistics.

    onNavigate(date) {
        this.setState({ date });
    }

    onView(obj) {
    }

    // Callback fired when the visible date range changes. Returns an Array of dates or an object
    // with start and end dates for BUILTIN views.
    onRangeChange(event, newView) {
        const { view } = this.state;
        let useView = newView;
        if (newView) {
            this.setState({ view: newView });
        } else {
            // newView isn't given (view not changed), so get view from state:
            useView = view;
        }
        const { start, end } = event;
        let newStart;
        let newEnd;
        if (useView === 'month' || useView === 'agenda') {
            newStart = start;
            newEnd = end;
        } else {
            const [element] = event;
            newStart = element;
        }
        this.setState({
            start: newStart,
            end: newEnd,
            view: useView,
        }, () => this.fetchEvents());
        // console.log("start:", myStart, "end", myEnd, useView)
    }

    // Callback fired when a calendar event is selected.
    onSelectEvent(event) {
        this.setState({
            editPanel: {
                visible: true,
                category: event.category,
                dbId: event.dbId,
                startDate: undefined,
                endDate: undefined,
            },
        });
    }

    // A callback fired when a date selection is made. Only fires when selectable is true.
    onSelectSlot(slotInfo) {
        const { calendar } = this.state;
        fetch(getServiceURL('calendar/action', {
            action: 'select',
            start: slotInfo.start ? slotInfo.start.toJSON() : '',
            end: slotInfo.end ? slotInfo.end.toJSON() : '',
            calendar,
        }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const { variables } = json;
                this.setState({
                    editPanel: {
                        visible: true,
                        category: variables.category,
                        startDate: variables.startDate,
                        endDate: variables.endDate,
                    },
                });
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    // Callback fired when a calendar event is clicked twice.
    onDoubleClickEvent() {

    }

    // Callback fired when dragging a selection in the Time views.
    // Returning false from the handler will prevent a selection.
    onSelecting(event) {
        console.log('onSelecting', event);
    }

    toggleEditModal() {
        this.setState(prevState => ({
            editPanel: {
                ...prevState.editPanel,
                visible: !prevState.editPanel.visible,
            },
        }));
    }

    convertJsonDates(e) {
        return Object.assign({}, e, {
            start: new Date(e.start),
            end: new Date(e.end),
        });
    }

    eventStyle(event) {
        const { viewType } = this.state;
        if (viewType === 'agenda') {
            return { // Don't change style for agenda:
                className: '',
            };
        }
        // Event is always undefined!!!
        const backgroundColor = (event && event.bgColor) ? event.bgColor : undefined;
        const textColor = (event && event.fgColor) ? event.fgColor : undefined;
        const cssClass = (event && event.cssClass) ? event.cssClass : undefined;
        return {
            style: {
                backgroundColor,
                color: textColor,
            },
            className: cssClass,
        };
    }

    navigateToDay(e) {
        console.log("*** ToDo: navigate to day.", e)
        this.setState({
            date: e,
            viewType: 'day',
        });
    }

    fetchEvents() {
        const { start, end, view } = this.state;
        const { activeCalendars } = this.props;
        const activeCalendarIds = activeCalendars ? activeCalendars.map(obj => obj.id) : [];
        this.setState({ loading: true });
        fetch(getServiceURL('calendar/events'), {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                start: start ? start.toJSON() : null,
                end: end ? end.toJSON() : null,
                view,
                activeCalendarIds,
            }),
        })
            .then(response => response.json())
            .then((json) => {
                const { events, specialDays } = json;
                this.setState({
                    loading: false,
                    events: events.map(this.convertJsonDates),
                    specialDays,
                });
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    render() {
        const { events, loading } = this.state;
        if (!events) {
            return (
                <LoadingContainer loading={loading}>
                    ...
                </LoadingContainer>
            );
        }
        const {
            date,
            view,
            editPanel,
            specialDays,
        } = this.state;
        const { topHeight } = this.props;
        const initTime = new Date(date.getDate());
        initTime.setHours(8);
        initTime.setMinutes(0);
        let editModalContent;
        if (editPanel.visible) {
            if (editPanel.category === 'timesheet') {
                editModalContent = (
                    <TimesheetEditPanel
                        timesheetId={editPanel.dbId ? editPanel.dbId.toString() : ''}
                        startDate={editPanel.startDate}
                        endDate={editPanel.endDate}
                    />
                );
            } else {
                editModalContent = <div>Event...</div>;
            }
        }
        return (
            <LoadingContainer loading={loading}>
                <DragAndDropCalendar
                    style={{
                        minHeight: 500,
                        height: `calc(100vh - ${topHeight})`,
                    }}
                    localizer={localizer}
                    events={events}
                    step={30}
                    view={view}
                    onView={this.onView}
                    views={['month', 'work_week', 'week', 'day', 'agenda']}
                    startAccessor="start"
                    date={date}
                    onNavigate={this.onNavigate}
                    endAccessor="end"
                    onRangeChange={this.onRangeChange}
                    onSelectEvent={this.onSelectEvent}
                    onSelectSlot={this.onSelectSlot}
                    selectable
                    eventPropGetter={this.eventStyle}
                    dayPropGetter={day => dayStyle(day, specialDays)}
                    showMultiDayTimes
                    timeslots={1}
                    scrollToTime={initTime}
                    components={{
                        event: renderEvent,
                        month: {
                            event: renderMonthEvent,
                            dateHeader: entry => renderDateHeader(entry, specialDays, this.navigateToDay),
                        },
                        week: {
                            // header: renderDateHeader
                        },
                        agenda: {
                            event: renderAgendaEvent,
                        },
                        toolbar: CalendarToolBar,
                    }}
                />
                <Modal
                    isOpen={editPanel.visible}
                    className="modal-xl"
                    toggle={this.toggleEditModal}
                    fade={false}
                >
                    <ModalBody>
                        {editModalContent}
                    </ModalBody>
                </Modal>

            </LoadingContainer>
        );
    }
}

CalendarPanel.propTypes = {
    activeCalendars: PropTypes.arrayOf(PropTypes.shape({})),
    firstDayOfWeek: PropTypes.number.isRequired,
    timeZone: PropTypes.string.isRequired,
    locale: PropTypes.string,
    topHeight: PropTypes.string,
    defaultDate: PropTypes.instanceOf(Date),
    defaultView: PropTypes.string,
};

CalendarPanel.defaultProps = {
    activeCalendars: [],
    locale: undefined,
    topHeight: '164px',
    defaultDate: new Date(),
    defaultView: 'month',
};

const mapStateToProps = ({ authentication }) => ({
    firstDayOfWeek: authentication.user.firstDayOfWeekNo,
    timeZone: authentication.user.timeZone,
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(CalendarPanel);
