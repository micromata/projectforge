/* eslint-disable no-alert */
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { SketchPicker } from 'react-color';
import { Card, CardBody, CardHeader } from 'reactstrap';
import CheckBox from '../../../components/design/input/CheckBox';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import { CalendarContext } from '../../page/calendar/CalendarContext';

/**
 * Picking colors for a calendar and switch visibility.
 */
class CalendarStyler extends Component {
    constructor(props) {
        super(props);
        const { calendar } = this.props;
        const background = (calendar && calendar.style && calendar.style.bgColor) ? calendar.style.bgColor : '#777';
        this.state = {
            background,
            visible: calendar.visible,
        };
        this.handleBackgroundColorChange = this.handleBackgroundColorChange.bind(this);
        this.handleVisibilityChange = this.handleVisibilityChange.bind(this);
    }

    handleBackgroundColorChange(color) {
        this.setState({ background: color.hex });
        const { calendar } = this.props;
        calendar.bgColor = color.hex;
    }

    handleVisibilityChange(event) {
        const { saveUpdateResponseInState } = this.context;

        this.setState({ visible: event.target.checked });
        const { calendar, submit } = this.props;
        fetch(getServiceURL('calendar/setVisibility', {
            calendarId: calendar.id,
            visible: event.target.checked,
        }), {
            method: 'GET',
            credentials: 'include',
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then(saveUpdateResponseInState)
            .then(() => {
                if (submit) {
                    submit();
                }
            })
            .catch((error) => alert(`Internal error: ${error}`));
    }

    render() {
        const { background, visible } = this.state;
        const { calendar, translations } = this.props;
        return (
            <Card>
                <CardHeader>
                    <h3>
                        { calendar.title }
                    </h3>
                </CardHeader>
                <CardBody>
                    <CheckBox
                        label={translations['calendar.filter.showCalendarEntries']}
                        id="opened"
                        onChange={this.handleVisibilityChange}
                        checked={visible}
                    />
                    <div style={{ marginLeft: '30px' }}>
                        <SketchPicker
                            color={background}
                            onChangeComplete={this.handleBackgroundColorChange}
                            disableAlpha
                        />
                    </div>
                </CardBody>
            </Card>
        );
    }
}

CalendarStyler.contextType = CalendarContext;

CalendarStyler.propTypes = {
    calendar: PropTypes.shape({
        id: PropTypes.number.isRequired,
        visible: PropTypes.bool,
        style: PropTypes.shape({
            bgColor: PropTypes.string,
        }),
        title: PropTypes.string,
        bgColor: PropTypes.string,
    }).isRequired,
    submit: PropTypes.func,
    translations: PropTypes.shape({
        'calendar.filter.showCalendarEntries': PropTypes.string,
    }).isRequired,
};

export default (CalendarStyler);
