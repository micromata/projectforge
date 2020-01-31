import { faCog } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Container, Popover, PopoverBody, PopoverHeader, Row } from 'reactstrap';
import ObjectSelect from '../../../components/design/input/autoCompletion/ObjectSelect';
import CheckBox from '../../../components/design/input/CheckBox';
import style from '../../../components/design/input/Input.module.scss';
import ReactSelect from '../../../components/design/ReactSelect';
import { fetchJsonGet, fetchJsonPost } from '../../../utilities/rest';
import { CalendarContext } from '../../page/calendar/CalendarContext';

/**
 * Settings of a calendar view: time sheet user, default calendar for new events, show holidays etc.
 */
class CalendarFilterSettings extends Component {
    static extractDefaultCalendarValue(props) {
        const { listOfDefaultCalendars, defaultCalendarId } = props;
        return listOfDefaultCalendars.find(it => it.id === defaultCalendarId);
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
            popoverOpen: false,
        };

        this.handleDefaultCalendarChange = this.handleDefaultCalendarChange.bind(this);
        this.handleTimesheetUserChange = this.handleTimesheetUserChange.bind(this);
        this.handleVacationGroupsChange = this.handleVacationGroupsChange.bind(this);
        this.handleVacationUsersChange = this.handleVacationUsersChange.bind(this);
        this.handleGridSizeChange = this.handleGridSizeChange.bind(this);
        this.togglePopover = this.togglePopover.bind(this);
        this.handleCheckBoxChange = this.handleCheckBoxChange.bind(this);
    }

    handleCheckBoxChange(event) {
        const { onTimesheetUserChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const user = { id: event.target.checked ? 1 : -1 };
        fetchJsonGet('calendar/changeTimesheetUser',
            { userId: user.id },
            (json) => {
                onTimesheetUserChange(user);
                saveUpdateResponseInState(json);
            });
    }

    handleTimesheetUserChange(user) {
        const { onTimesheetUserChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const userId = user ? user.id : undefined;
        fetchJsonGet('calendar/changeTimesheetUser',
            { userId },
            (json) => {
                onTimesheetUserChange(user);
                saveUpdateResponseInState(json);
            });
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
        fetchJsonGet('calendar/changeGridSizer',
            { size },
            (json) => {
                onGridSizeChange(size);
                saveUpdateResponseInState(json);
            });
    }

    handleDefaultCalendarChange(value) {
        const { onDefaultCalendarChange } = this.props;
        const { saveUpdateResponseInState } = this.context;
        const id = value ? value.id : '';
        fetchJsonGet('calendar/changeDefaultCalendar',
            { id },
            (json) => {
                onDefaultCalendarChange(id);
                saveUpdateResponseInState(json);
            });
    }

    togglePopover() {
        this.setState(prevState => ({
            popoverOpen: !prevState.popoverOpen,
        }));
    }

    render() {
        const { popoverOpen } = this.state;
        const {
            listOfDefaultCalendars,
            otherTimesheetUsersEnabled,
            timesheetUser,
            gridSize,
            translations,
            vacationGroups,
            vacationUsers,
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
            label: gridSize,
        };
        const defaultCalendar = CalendarFilterSettings.extractDefaultCalendarValue(this.props);
        return (
            <React.Fragment>
                <Button
                    id="calendarSettingsPopover"
                    color="link"
                    className="selectPanelIconLinks"
                    onClick={this.togglePopover}
                >
                    <FontAwesomeIcon
                        icon={faCog}
                        className={style.icon}
                        size="lg"
                    />
                </Button>
                <Popover
                    placement="bottom-end"
                    isOpen={popoverOpen}
                    target="calendarSettingsPopover"
                    toggle={this.togglePopover}
                    trigger="legacy"
                >
                    <PopoverHeader>
                        {translations.settings}
                    </PopoverHeader>
                    <PopoverBody>
                        <Container>
                            <Row>
                                <Col>
                                    <ReactSelect
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
                                <Col>
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
                                            onChange={this.handleCheckBoxChange}
                                            checked={timesheetUser && timesheetUser.id > 0}
                                        />
                                    )}
                                </Col>
                            </Row>
                            <Row>
                                <Col>
                                    <ReactSelect
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
                                        loadOptions={
                                            CalendarFilterSettings.loadVacationUsersOptions
                                        }
                                        value={vacationUsers}
                                        label={translations['calendar.filter.vacation.users']}
                                        tooltip={translations['calendar.filter.vacation.user.tooltip']}
                                        translations={translations}
                                        valueProperty="id"
                                        labelProperty="displayName"
                                        multi
                                        onChange={this.handleVacationUsersChange}
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>
                                    <ReactSelect
                                        values={gridSizes}
                                        value={gridSizeValue}
                                        label={translations['calendar.option.gridSize']}
                                        translations={translations}
                                        onChange={this.handleGridSizeChange}
                                    />
                                </Col>
                            </Row>
                        </Container>
                    </PopoverBody>
                </Popover>
            </React.Fragment>
        );
    }
}

CalendarFilterSettings.contextType = CalendarContext;

CalendarFilterSettings.propTypes = {
    onTimesheetUserChange: PropTypes.func.isRequired,
    onDefaultCalendarChange: PropTypes.func.isRequired,
    onGridSizeChange: PropTypes.func.isRequired,
    onVacationGroupsChange: PropTypes.func.isRequired,
    onVacationUsersChange: PropTypes.func.isRequired,
    /* eslint-disable-next-line react/no-unused-prop-types */
    defaultCalendarId: PropTypes.number,
    timesheetUser: PropTypes.shape(),
    gridSize: PropTypes.number,
    otherTimesheetUsersEnabled: PropTypes.bool.isRequired,
    listOfDefaultCalendars: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    translations: PropTypes.shape({}).isRequired,
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
    gridSize: 30,
    vacationGroups: [],
    vacationUsers: [],
};

export default (CalendarFilterSettings);
