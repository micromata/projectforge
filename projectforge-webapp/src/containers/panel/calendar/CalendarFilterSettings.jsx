import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Container, Popover, PopoverBody, PopoverHeader, Row, } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome/index';
import { faCog } from '@fortawesome/free-solid-svg-icons';
import style from '../../../components/design/input/Input.module.scss';
import UncontrolledReactSelect from '../../../components/base/page/layout/UncontrolledReactSelect';

/**
 * Settings of a calendar view: time sheet user, default calendar for new events, show holidays etc.
 */
class CalendarFilterSettings extends Component {
    constructor(props) {
        super(props);
        this.state = {
            popoverOpen: false,
        };

        this.togglePopover = this.togglePopover.bind(this);
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
                    placement="bottom-start"
                    isOpen={popoverOpen}
                    target="calendarSettingsPopover"
                    toggle={this.togglePopover}
                    trigger="legacy"
                >
                    <PopoverHeader toggle={this.togglePopover}>
                        {translations.favorites}
                    </PopoverHeader>
                    <PopoverBody>
                        <Container>
                            <Row>
                                <Col>
                                    <UncontrolledReactSelect
                                        label={translations['calendar.defaultCalendar']}
                                        tooltip={translations['calendar.defaultCalendar.tooltip']}
                                        // data={data}
                                        id="id"
                                        values={listOfDefaultCalendars}
                                        changeDataField={this.changeDefaultCalendar}
                                        translations={translations}
                                        valueProperty="id"
                                        labelProperty="title"
                                    />
                                </Col>
                            </Row>
                            <Row>
                                <Col>Zeitberichtsuser</Col>
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
    listOfDefaultCalendars: PropTypes.arrayOf({}).isRequired,
    translations: PropTypes.shape({}).isRequired,
};

CalendarFilterSettings.defaultProps = {
    translations: [],
};

export default (CalendarFilterSettings);
