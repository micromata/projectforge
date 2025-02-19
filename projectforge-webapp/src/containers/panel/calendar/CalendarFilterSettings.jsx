import { faChevronDown, faCog, faEllipsis } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import PropTypes from 'prop-types';
import React, { useContext, useEffect, useState } from 'react';
import {
    Button,
    Col,
    Container,
    DropdownItem, DropdownMenu,
    DropdownToggle,
    Modal,
    ModalBody,
    ModalHeader, Navbar, NavLink,
    Row, UncontrolledDropdown,
    UncontrolledTooltip,
} from 'reactstrap';
import ObjectSelect from '../../../components/design/input/autoCompletion/ObjectSelect';
import CheckBox from '../../../components/design/input/CheckBox';
import style from '../../../components/design/input/Input.module.scss';
import ReactSelect from '../../../components/design/react-select/ReactSelect';
import { fetchJsonGet, fetchJsonPost, getServiceURL } from '../../../utilities/rest';
import prefix from '../../../utilities/prefix';
import { CalendarContext } from '../../page/calendar/CalendarContext';
import { Nav } from '../../../components/design';
import history from '../../../utilities/history';

/**
 * Settings of a calendar view: time sheet user, default calendar for new events, show holidays etc.
 */
function CalendarFilterSettings({
    onTimesheetUserChange,
    onShowBreaksChange,
    onDefaultCalendarChange,
    onGridSizeChange,
    onFirstHourChange,
    onVacationGroupsChange,
    onVacationUsersChange,
    refresh,
    defaultCalendarId,
    timesheetUser,
    otherTimesheetUsersEnabled,
    showBreaks,
    gridSize = '30',
    firstHour = '08',
    listOfDefaultCalendars,
    translations,
    vacationGroups = [],
    vacationUsers = [],
}) {
    // Modal dialog with calendars, vacations and other option such first hour of day etc.
    const [open, setOpen] = useState(false);

    const [defaultCalendar, setDefaultCalendar] = useState();

    useEffect(() => {
        setDefaultCalendar(listOfDefaultCalendars.find((it) => it.id === defaultCalendarId));
    }, [listOfDefaultCalendars, defaultCalendarId]);

    const { saveUpdateResponseInState } = useContext(CalendarContext);

    const loadVacationGroupsOptions = (search, callback) => {
        fetchJsonGet(
            'group/autosearch',
            { search },
            callback,
        );
    };

    const loadVacationUsersOptions = (search, callback) => {
        fetchJsonGet(
            'vacation/users',
            { search },
            callback,
        );
    };

    const handleTimesheetsCheckBoxChange = (event) => {
        const user = { id: event.target.checked ? 1 : -1 };
        fetchJsonGet(
            'calendar/changeTimesheetUser',
            { userId: user.id },
            (json) => {
                onTimesheetUserChange(user);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleShowBreaksChange = (event) => {
        const { checked } = event.target;
        fetchJsonGet(
            'calendar/changeShowBreaks',
            { showBreaks },
            (json) => {
                onShowBreaksChange(checked);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleTimesheetUserChange = (user) => {
        const userId = user ? user.id : undefined;
        fetchJsonGet(
            'calendar/changeTimesheetUser',
            { userId },
            (json) => {
                onTimesheetUserChange(user);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleVacationGroupsChange = (groups) => {
        fetchJsonPost(
            'calendar/changeVacationGroups',
            (groups || []).map(({ id }) => id),
            (json) => {
                onVacationGroupsChange(groups);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleVacationUsersChange = (users) => {
        fetchJsonPost(
            'calendar/changeVacationUsers',
            (users || []).map(({ id }) => id),
            (json) => {
                onVacationUsersChange(users);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleGridSizeChange = (newGridSize) => {
        const size = newGridSize ? newGridSize.value : 30;
        fetchJsonGet(
            'calendar/changeGridSize',
            { size },
            (json) => {
                onGridSizeChange(size);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleFirstHourChange = (newFirstHour) => {
        const hour = newFirstHour ? newFirstHour.value : 8;
        fetchJsonGet(
            'calendar/changeFirstHour',
            { hour },
            (json) => {
                onFirstHourChange(hour);
                saveUpdateResponseInState(json);
            },
        );
    };

    const handleDefaultCalendarChange = (value) => {
        const id = value ? value.id : '';
        fetchJsonGet(
            'calendar/changeDefaultCalendar',
            { id },
            (json) => {
                onDefaultCalendarChange(id);
                saveUpdateResponseInState(json);
            },
        );
    };

    const toggle = () => {
        setOpen(!open);
    };

    const gridSizes = [{
        value: 5,
        label: '5',
    }, {
        value: 10,
        label: '10',
    }, {
        value: 15,
        label: '15',
    }, {
        value: 30,
        label: '30',
    }, {
        value: 60,
        label: '60',
    }];
    const gridSizeValue = {
        value: gridSize,
        label: gridSize.toString(),
    };
    const firstHours = [];
    for (let i = 0; i < 24; i += 1) {
        firstHours.push({
            value: i,
            label: `${Number.as2Digits(i)}:00`,
        });
    }
    const firstHourValue = {
        value: firstHour,
        label: `${Number.as2Digits(firstHour)}:00`,
    };
    return (
        <>
            <Button
                color="link"
                id="calendar-view-settings"
                className="selectPanelIconLinks"
                onClick={toggle}
            >
                <FontAwesomeIcon
                    icon={faCog}
                    className={style.icon}
                    size="lg"
                />
            </Button>
            <UncontrolledTooltip placement="left" target="calendar-view-settings">
                {translations['calendar.view.settings.tooltip']}
            </UncontrolledTooltip>
            <Navbar style={{ marginLeft: '0 !important', padding: '0' }}>
                <Nav className="ml-auto">
                    <UncontrolledDropdown nav>
                        <DropdownToggle nav style={{ marginLeft: '0 !important', padding: '0' }}>
                            <div id="more-menu" style={{ whiteSpace: 'nowrap' }}>
                                <FontAwesomeIcon
                                    icon={faEllipsis}
                                    className={style.icon}
                                    size="lg"
                                />
                                <FontAwesomeIcon icon={faChevronDown} />
                            </div>
                            <UncontrolledTooltip placement="top" target="more-menu">
                                {translations.more}
                            </UncontrolledTooltip>
                        </DropdownToggle>
                        <DropdownMenu end>
                            <DropdownItem
                                key="entry-item-calendar-settings"
                            >
                                <NavLink
                                    id="calendarSettings"
                                    onClick={() => history.push(`${prefix}calendar/calendarSettings/dynamic`)}
                                    rel="noopener noreferrer"
                                >
                                    {translations['calendar.settings'] || '[Calendar settings]'}
                                </NavLink>
                            </DropdownItem>
                            <DropdownItem
                                key="entry-item-calendar-refresh"
                            >
                                <NavLink
                                    id="calendar-refresh"
                                    onClick={refresh}
                                >
                                    {translations.reload || '[reload]'}
                                </NavLink>
                                <UncontrolledTooltip placement="left" target="calendar-refresh">
                                    {translations['plugins.teamcal.calendar.refresh.tooltip']}
                                </UncontrolledTooltip>
                            </DropdownItem>
                            <DropdownItem
                                key="entry-item-calendar-list"
                            >
                                <NavLink
                                    id="calendarlist"
                                    href={getServiceURL(`${prefix}teamCal`)}
                                    rel="noopener noreferrer"
                                >
                                    {translations['menu.plugins.teamcal'] || '[List of calendars]'}
                                </NavLink>
                                <UncontrolledTooltip placement="left" target="calendarlist">
                                    {translations['plugins.teamcal.calendar.listAndIcsExport.tooltip']}
                                </UncontrolledTooltip>
                            </DropdownItem>
                        </DropdownMenu>
                    </UncontrolledDropdown>
                </Nav>
            </Navbar>
            <Modal
                isOpen={open}
                toggle={toggle}
            >
                <ModalHeader toggle={toggle}>
                    {translations.settings}
                </ModalHeader>
                <ModalBody>
                    <Container>
                        <Row>
                            <Col>
                                <ReactSelect
                                    id="defaultCalendar"
                                    values={listOfDefaultCalendars}
                                    value={defaultCalendar}
                                    label={translations['calendar.defaultCalendar']}
                                    tooltip={translations['calendar.defaultCalendar.tooltip']}
                                    translations={translations}
                                    valueProperty="id"
                                    labelProperty="title"
                                    onChange={handleDefaultCalendarChange}
                                />
                            </Col>
                        </Row>
                        <Row>
                            <Col md={6}>
                                {otherTimesheetUsersEnabled ? (
                                    <ObjectSelect
                                        id="showTimesheets"
                                        label={translations['calendar.option.timesheets']}
                                        onSelect={handleTimesheetUserChange}
                                        translations={translations}
                                        type="USER"
                                        value={timesheetUser}
                                    />
                                ) : (
                                    <CheckBox
                                        label={translations['calendar.option.timesheets']}
                                        id="showTimesheets"
                                        onChange={handleTimesheetsCheckBoxChange}
                                        checked={timesheetUser && timesheetUser.id > 0}
                                    />
                                )}
                            </Col>
                            <Col md={6}>
                                <CheckBox
                                    id="showBreaks"
                                    label={translations['calendar.option.showBreaks']}
                                    tooltip={translations['calendar.option.showBreaks.tooltip']}
                                    onChange={handleShowBreaksChange}
                                    checked={showBreaks}
                                />
                            </Col>
                        </Row>
                        <Row>
                            <Col>
                                <ReactSelect
                                    id="vacationGroups"
                                    loadOptions={loadVacationGroupsOptions}
                                    value={vacationGroups}
                                    label={translations['calendar.filter.vacation.groups']}
                                    tooltip={translations['calendar.filter.vacation.groups.tooltip']}
                                    translations={translations}
                                    valueProperty="id"
                                    labelProperty="displayName"
                                    multi
                                    onChange={handleVacationGroupsChange}
                                />
                            </Col>
                        </Row>
                        <Row>
                            <Col>
                                <ReactSelect
                                    id="vacationUsers"
                                    loadOptions={loadVacationUsersOptions}
                                    value={vacationUsers}
                                    label={translations['calendar.filter.vacation.users']}
                                    tooltip={translations['calendar.filter.vacation.users.tooltip']}
                                    translations={translations}
                                    valueProperty="id"
                                    labelProperty="displayName"
                                    multi
                                    onChange={handleVacationUsersChange}
                                />
                            </Col>
                        </Row>
                        <Row>
                            <Col md={6}>
                                <ReactSelect
                                    id="gridSize"
                                    values={gridSizes}
                                    value={gridSizeValue}
                                    label={translations['calendar.option.gridSize']}
                                    tooltip={translations['calendar.option.gridSize.tooltip']}
                                    translations={translations}
                                    onChange={handleGridSizeChange}
                                />
                            </Col>
                            <Col md={6}>
                                <ReactSelect
                                    id="firstHour"
                                    values={firstHours}
                                    value={firstHourValue}
                                    label={translations['calendar.option.firstHour']}
                                    tooltip={translations['calendar.option.firstHour.tooltip']}
                                    translations={translations}
                                    onChange={handleFirstHourChange}
                                />
                            </Col>
                        </Row>
                    </Container>
                </ModalBody>
            </Modal>
        </>
    );
}

CalendarFilterSettings.propTypes = {
    onTimesheetUserChange: PropTypes.func.isRequired,
    onShowBreaksChange: PropTypes.func.isRequired,
    onDefaultCalendarChange: PropTypes.func.isRequired,
    onGridSizeChange: PropTypes.func.isRequired,
    onFirstHourChange: PropTypes.func.isRequired,
    onVacationGroupsChange: PropTypes.func.isRequired,
    onVacationUsersChange: PropTypes.func.isRequired,
    refresh: PropTypes.func.isRequired,
    defaultCalendarId: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
    ]),
    timesheetUser: PropTypes.shape(),
    otherTimesheetUsersEnabled: PropTypes.bool,
    showBreaks: PropTypes.bool,
    gridSize: PropTypes.number,
    firstHour: PropTypes.number,
    listOfDefaultCalendars: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    translations: PropTypes.shape({
        more: PropTypes.string,
        reload: PropTypes.string,
        settings: PropTypes.string,
        'calendar.defaultCalendar': PropTypes.string,
        'calendar.defaultCalendar.tooltip': PropTypes.string,
        'calendar.option.showBreaks': PropTypes.string,
        'calendar.option.showBreaks.tooltip': PropTypes.string,
        'calendar.option.timesheets': PropTypes.string,
        'calendar.filter.vacation.groups': PropTypes.string,
        'calendar.filter.vacation.groups.tooltip': PropTypes.string,
        'calendar.filter.vacation.users': PropTypes.string,
        'calendar.filter.vacation.users.tooltip': PropTypes.string,
        'calendar.view.oldVersion': PropTypes.string,
        'calendar.view.oldVersion.tooltip': PropTypes.string,
        'calendar.option.firstHour': PropTypes.string,
        'calendar.option.firstHour.tooltip': PropTypes.string,
        'calendar.option.gridSize': PropTypes.string,
        'calendar.option.gridSize.tooltip': PropTypes.string,
        'calendar.settings': PropTypes.string,
        'calendar.view.settings.tooltip': PropTypes.string,
        'menu.plugins.teamcal': PropTypes.string,
        'plugins.teamcal.calendar.listAndIcsExport.tooltip': PropTypes.string,
        'plugins.teamcal.calendar.refresh.tooltip': PropTypes.string,
    }).isRequired,
    vacationGroups: PropTypes.arrayOf(PropTypes.shape({
        title: PropTypes.string,
        id: PropTypes.number,
    })),
    vacationUsers: PropTypes.arrayOf(PropTypes.shape({
        title: PropTypes.string,
        id: PropTypes.number,
    })),
};

export default (CalendarFilterSettings);
