import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Container, Popover, PopoverBody, PopoverHeader, Row, } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import style from '../../../components/design/input/Input.module.scss';
import ReactSelect from '../../../components/design/ReactSelect';
import UserSelect from '../../../components/base/page/layout/UserSelect';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';

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
        const defaultCalendar = CalendarFilterSettings.extractDefaultCalendarValue(props);
        this.state = {
            defaultCalendar,
            popoverOpen: false,
        };

        this.onDefaultCalendarChange = this.onDefaultCalendarChange.bind(this);
        this.onTimesheetUserChange = this.onTimesheetUserChange.bind(this);
        this.togglePopover = this.togglePopover.bind(this);
    }

    onDefaultCalendarChange(value) {
        const id = value ? value.id : '';
        this.setState({ defaultCalendar: value });
        fetch(getServiceURL('calendar/changeDefaultCalendar',
            { id }), {
            method: 'GET',
            credentials: 'include',
        })
            .then(handleHTTPErrors)
            .catch(error => alert(`Internal error: ${error}`));
    }

    onTimesheetUserChange(id, value) {
        const { onTimesheetUserChange } = this.props;
        const userId = value ? value.id : '';
        fetch(getServiceURL('calendar/changeTimesheetUser',
            { userId }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then(() => onTimesheetUserChange(userId))
            .catch(error => alert(`Internal error: ${error}`));
    }

    togglePopover() {
        this.setState(prevState => ({
            popoverOpen: !prevState.popoverOpen,
        }));
    }

    render() {
        const { defaultCalendar, popoverOpen } = this.state;
        const {
            listOfDefaultCalendars,
            translations,
        } = this.props;
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
                                        onChange={this.onDefaultCalendarChange}
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>
                                    <UserSelect
                                        changeDataField={this.onTimesheetUserChange}
                                        id="timesheetUser"
                                        data={this.props}
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
    /* eslint-disable-next-line react/no-unused-prop-types */
    defaultCalendarId: PropTypes.number,
    listOfDefaultCalendars: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    translations: PropTypes.shape({}).isRequired,
};

CalendarFilterSettings.defaultProps = {
    defaultCalendarId: -1,
};

export default (CalendarFilterSettings);
