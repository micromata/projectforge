import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Container, Popover, PopoverBody, PopoverHeader, Row, } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import style from '../../../components/design/input/Input.module.scss';
import ReactSelect from '../../../components/design/ReactSelect';
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
        const defaultCalendar = CalendarFilterSettings.extractDefaultCalendarValue(props);
        this.state = {
            defaultCalendar,
            popoverOpen: false,
            timesheetUser: undefined,
        };

        this.onDefaultCalendarChange = this.onDefaultCalendarChange.bind(this);
        this.onTimesheetUserChange = this.onTimesheetUserChange.bind(this);
        this.togglePopover = this.togglePopover.bind(this);
    }

    onDefaultCalendarChange(value) {
        this.setState({ defaultCalendar: value });
    }

    onTimesheetUserChange(id, value) {
        console.log(id, value);
        this.setState({ timesheetUser: value });
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
                        {translations.favorites}
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
                                        label="[timesheetUser]"
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
    /* eslint-disable-next-line react/no-unused-prop-types */
    defaultCalendarId: PropTypes.number,
    listOfDefaultCalendars: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
    translations: PropTypes.shape({}).isRequired,
};

CalendarFilterSettings.defaultProps = {
    defaultCalendarId: -1,
};

export default (CalendarFilterSettings);
