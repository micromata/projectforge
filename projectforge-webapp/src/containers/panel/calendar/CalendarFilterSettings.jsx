import { faCog, faArrowsRotate, faList } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Container, Modal, ModalBody, ModalHeader, Row, UncontrolledTooltip } from 'reactstrap';
import ObjectSelect from '../../../components/design/input/autoCompletion/ObjectSelect';
import CheckBox from '../../../components/design/input/CheckBox';
import style from '../../../components/design/input/Input.module.scss';
import ReactSelect from '../../../components/design/react-select/ReactSelect';
import { fetchJsonGet, fetchJsonPost } from '../../../utilities/rest';
import history from '../../../utilities/history';
import prefix from '../../../utilities/prefix';
import { CalendarContext } from '../../page/calendar/CalendarContext';

/**
 * Settings of a calendar view: time sheet user, default calendar for new events, show holidays etc.
 */
class CalendarFilterSettings extends Component {
    static extractDefaultCalendarValue(props) {
        const { listOfDefaultCalendars, defaultCalendarId } = props;
        return listOfDefaultCalendars.find((it) => it.id === defaultCalendarId);
    }

    static loadVacationGroupsOptions(search, callback) {
        fetchJsonGet(
            'group/autosearch',
            { search },
            callback,
        );
    }

    static loadVacationUsersOptions(search, callback) {
        fetchJsonGet(
            'vacation/users',
            { search },
            callback,
        );
    }

    constructor(props) {
        super(props);
        this.state = {
            isOpen: false,
        };

        this.handleDefaultCalendarChange = this.handleDefaultCalendarChange.bind(this);
        this.handleTimesheetUserChange = this.handleTimesheetUserChange.bind(this);
        this.handleVacationGroupsChange = this.handleVacationGroupsChange.bind(this);
        this.handleVacationUsersChange = this.handleVacationUsersChange.bind(this);
        this.handleGridSizeChange = this.handleGridSizeChange.bind(this);
        this.handleFirstHourChange = this.handleFirstHourChange.bind(this);
        this.toggle = this.toggle.bind(this);
        this.handleTimesheetsCheckBoxChange = this.handleTimesheetsCheckBoxChange.bind(this);
        this.handleShowBreaksChange = this.handleShowBreaksChange.bind(this);
    }

    handleTimesheetsCheckBoxChange(event) {
        const { onTimesheetUserChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const user = { id: event.target.checked ? 1 : -1 };
        fetchJsonGet(
            'calendar/changeTimesheetUser',
            { userId: user.id },
            (json) => {
                onTimesheetUserChange(user);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleShowBreaksChange(event) {
        const { onShowBreaksChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const showBreaks = event.target.checked;
        fetchJsonGet(
            'calendar/changeShowBreaks',
            { showBreaks },
            (json) => {
                onShowBreaksChange(showBreaks);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleTimesheetUserChange(user) {
        const { onTimesheetUserChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const userId = user ? user.id : undefined;
        fetchJsonGet(
            'calendar/changeTimesheetUser',
            { userId },
            (json) => {
                onTimesheetUserChange(user);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleVacationGroupsChange(groups) {
        fetchJsonPost(
            'calendar/changeVacationGroups',
            (groups || []).map(({ id }) => id),
            (json) => {
                const { saveUpdateResponseInState } = this.context;
                const { onVacationGroupsChange } = this.props;
                onVacationGroupsChange(groups);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleVacationUsersChange(users) {
        fetchJsonPost(
            'calendar/changeVacationUsers',
            (users || []).map(({ id }) => id),
            (json) => {
                const { saveUpdateResponseInState } = this.context;
                const { onVacationUsersChange } = this.props;
                onVacationUsersChange(users);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleGridSizeChange(gridSize) {
        const { onGridSizeChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const size = gridSize ? gridSize.value : 30;
        fetchJsonGet(
            'calendar/changeGridSize',
            { size },
            (json) => {
                onGridSizeChange(size);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleFirstHourChange(firstHour) {
        const { onFirstHourChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const hour = firstHour ? firstHour.value : 8;
        fetchJsonGet(
            'calendar/changeFirstHour',
            { hour },
            (json) => {
                onFirstHourChange(hour);
                saveUpdateResponseInState(json);
            },
        );
    }

    handleDefaultCalendarChange(value) {
        const { onDefaultCalendarChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const id = value ? value.id : '';
        fetchJsonGet(
            'calendar/changeDefaultCalendar',
            { id },
            (json) => {
                onDefaultCalendarChange(id);
                saveUpdateResponseInState(json);
            },
        );
    }

    toggle() {
        this.setState((prevState) => ({
            isOpen: !prevState.isOpen,
        }));
    }

    render() {
        const { isOpen } = this.state;
        const {
            listOfDefaultCalendars,
            otherTimesheetUsersEnabled,
            timesheetUser,
            showBreaks,
            gridSize,
            firstHour,
            translations,
            vacationGroups,
            vacationUsers,
            refresh,
        } = this.props;
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
        const defaultCalendar = CalendarFilterSettings.extractDefaultCalendarValue(this.props);
        return (
            <>
                <Button
                    color="link"
                    id="calendar-view-settings"
                    className="selectPanelIconLinks"
                    onClick={this.toggle}
                >
                    <FontAwesomeIcon
                        icon={faCog}
                        className={style.icon}
                        size="lg"
                    />
                </Button>
                <UncontrolledTooltip placement="bottom" target="calendar-view-settings">
                    {translations['calendar.view.settings.tooltip']}
                </UncontrolledTooltip>
                <Button
                    color="link"
                    id="calendar-refresh"
                    className="selectPanelIconLinks"
                    onClick={refresh}
                >
                    <FontAwesomeIcon
                        icon={faArrowsRotate}
                        className={style.icon}
                        size="lg"
                    />
                </Button>
                <UncontrolledTooltip placement="bottom" target="calendar-refresh">
                    {translations['plugins.teamcal.calendar.refresh.tooltip']}
                </UncontrolledTooltip>
                <Button
                    color="link"
                    id="calendarlist"
                    className="selectPanelIconLinks"
                    onClick={() => history.push(`${prefix}teamCal`)}
                >
                    <FontAwesomeIcon
                        icon={faList}
                        className={style.icon}
                        size="lg"
                    />
                </Button>
                <UncontrolledTooltip placement="bottom" target="calendarlist">
                    {translations['plugins.teamcal.calendar.listAndIcsExport.tooltip']}
                </UncontrolledTooltip>
                <Modal
                    isOpen={isOpen}
                    toggle={this.toggle}
                >
                    <ModalHeader toggle={this.toggle}>
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
                                        onChange={this.handleDefaultCalendarChange}
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col md={6}>
                                    {otherTimesheetUsersEnabled ? (
                                        <ObjectSelect
                                            id="showTimesheets"
                                            label={translations['calendar.option.timesheets']}
                                            onSelect={this.handleTimesheetUserChange}
                                            translations={translations}
                                            type="USER"
                                            value={timesheetUser}
                                        />
                                    ) : (
                                        <CheckBox
                                            label={translations['calendar.option.timesheets']}
                                            id="showTimesheets"
                                            onChange={this.handleTimesheetsCheckBoxChange}
                                            checked={timesheetUser && timesheetUser.id > 0}
                                        />
                                    )}
                                </Col>
                                <Col md={6}>
                                    <CheckBox
                                        id="showBreaks"
                                        label={translations['calendar.option.showBreaks']}
                                        tooltip={translations['calendar.option.showBreaks.tooltip']}
                                        onChange={this.handleShowBreaksChange}
                                        checked={showBreaks}
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>
                                    <ReactSelect
                                        id="vacationGroups"
                                        loadOptions={
                                            CalendarFilterSettings.loadVacationGroupsOptions
                                        }
                                        value={vacationGroups}
                                        label={translations['calendar.filter.vacation.groups']}
                                        tooltip={translations['calendar.filter.vacation.groups.tooltip']}
                                        translations={translations}
                                        valueProperty="id"
                                        labelProperty="displayName"
                                        multi
                                        onChange={this.handleVacationGroupsChange}
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>
                                    <ReactSelect
                                        id="vacationUsers"
                                        loadOptions={
                                            CalendarFilterSettings.loadVacationUsersOptions
                                        }
                                        value={vacationUsers}
                                        label={translations['calendar.filter.vacation.users']}
                                        tooltip={translations['calendar.filter.vacation.users.tooltip']}
                                        translations={translations}
                                        valueProperty="id"
                                        labelProperty="displayName"
                                        multi
                                        onChange={this.handleVacationUsersChange}
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
                                        onChange={this.handleGridSizeChange}
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
                                        onChange={this.handleFirstHourChange}
                                    />
                                </Col>
                            </Row>
                        </Container>
                    </ModalBody>
                </Modal>
            </>
        );
    }
}

CalendarFilterSettings.contextType = CalendarContext;

CalendarFilterSettings.propTypes = {
    onTimesheetUserChange: PropTypes.func.isRequired,
    onShowBreaksChange: PropTypes.func.isRequired,
    onDefaultCalendarChange: PropTypes.func.isRequired,
    onGridSizeChange: PropTypes.func.isRequired,
    onFirstHourChange: PropTypes.func.isRequired,
    onVacationGroupsChange: PropTypes.func.isRequired,
    onVacationUsersChange: PropTypes.func.isRequired,
    refresh: PropTypes.func.isRequired,
    /* eslint-disable-next-line react/no-unused-prop-types */
    defaultCalendarId: PropTypes.number,
    timesheetUser: PropTypes.shape(),
    otherTimesheetUsersEnabled: PropTypes.bool,
    showBreaks: PropTypes.bool,
    gridSize: PropTypes.number,
    firstHour: PropTypes.number,
    listOfDefaultCalendars: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    translations: PropTypes.shape({
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
        'calendar.option.firstHour': PropTypes.string,
        'calendar.option.firstHour.tooltip': PropTypes.string,
        'calendar.option.gridSize': PropTypes.string,
        'calendar.option.gridSize.tooltip': PropTypes.string,
        'calendar.view.settings.tooltip': PropTypes.string,
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

CalendarFilterSettings.defaultProps = {
    defaultCalendarId: undefined,
    timesheetUser: undefined,
    otherTimesheetUsersEnabled: undefined,
    showBreaks: undefined,
    gridSize: '30',
    firstHour: '08',
    vacationGroups: [],
    vacationUsers: [],
};

export default (CalendarFilterSettings);
