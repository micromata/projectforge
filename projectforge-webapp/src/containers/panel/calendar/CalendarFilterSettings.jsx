import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Container, Popover, PopoverBody, PopoverHeader, Row, } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import style from '../../../components/design/input/Input.module.scss';
import ReactSelect from '../../../components/design/ReactSelect';
import { fetchGet } from '../../../utilities/rest';
import UserSelect from '../../../components/base/page/layout/UserSelect';

/**
 * Settings of a calendar view: time sheet user, default calendar for new events, show holidays etc.
 */
class CalendarFilterSettings extends Component {
    static extractDefaultCalendarValue(props) {
        const { listOfDefaultCalendars, defaultCalendarId } = props;
        return listOfDefaultCalendars.find(it => it.id === defaultCalendarId);
    }

    constructor(props) {
        super(props);
        this.state = {
            popoverOpen: false,
        };

        this.handleDefaultCalendarChange = this.handleDefaultCalendarChange.bind(this);
        this.handleTimesheetUserChange = this.handleTimesheetUserChange.bind(this);
        this.togglePopover = this.togglePopover.bind(this);
    }

    handleTimesheetUserChange(user) {
        const { onTimesheetUserChange } = this.props;
        const userId = user ? user.id : undefined;
        fetchGet('calendar/changeTimesheetUser',
            () => onTimesheetUserChange(user),
            { userId });
    }

    handleDefaultCalendarChange(value) {
        const { onDefaultCalendarChange } = this.props;
        const id = value ? value.id : '';
        fetchGet('calendar/changeDefaultCalendar',
            () => onDefaultCalendarChange(id),
            { id });
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
            timesheetUser,
            translations,
        } = this.props;
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
                                    <UserSelect
                                        onChange={this.handleTimesheetUserChange}
                                        value={timesheetUser}
                                        label={translations['timesheet.user']}
                                        translations={translations}
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>
                                    Optionen: Pausen, Statistik,
                                    Geburtstage, Planungen
                                </Col>
                            </Row>
                        </Container>
                    </PopoverBody>
                </Popover>
            </React.Fragment>
        );
    }
}

CalendarFilterSettings.propTypes = {
    onTimesheetUserChange: PropTypes.func.isRequired,
    onDefaultCalendarChange: PropTypes.func.isRequired,
    /* eslint-disable-next-line react/no-unused-prop-types */
    defaultCalendarId: PropTypes.number,
    timesheetUser: PropTypes.shape(),
    listOfDefaultCalendars: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    translations: PropTypes.shape({}).isRequired,
};

CalendarFilterSettings.defaultProps = {
    defaultCalendarId: undefined,
    timesheetUser: undefined,
};

export default (CalendarFilterSettings);
