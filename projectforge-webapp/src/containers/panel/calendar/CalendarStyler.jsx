import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { SketchPicker } from 'react-color';
import CheckBox from '../../../components/design/input/CheckBox';
import { getServiceURL } from '../../../utilities/rest';

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
        this.setState({ visible: event.target.checked });
        const { calendar, submit } = this.props;
        fetch(getServiceURL('calendar/setVisibility', {
            calendarId: calendar.id,
            visible: event.target.checked,
        }), {
            method: 'GET',
            credentials: 'include',
        })
            .then(() => {
                if (submit) submit();

                // TODO ONLY RELOAD THE DATA
                window.location.reload();
            })
            .catch(error => alert(`Internal error: ${error}`));
    }

    render() {
        const { background, visible } = this.state;
        const { calendar } = this.props;
        return (
            <React.Fragment>
                <CheckBox
                    // label={translations['calendar.filter.visible']}
                    label={calendar.title}
                    id="opened"
                    onChange={this.handleVisibilityChange}
                    checked={visible}
                />
                <SketchPicker
                    color={background}
                    onChangeComplete={this.handleBackgroundColorChange}
                    disableAlpha
                />
            </React.Fragment>
        );
    }
}

CalendarStyler.propTypes = {
    calendar: PropTypes.shape({
        id: PropTypes.number.isRequired,
        visible: PropTypes.bool,
        style: PropTypes.shape({
            bgColor: PropTypes.string,
        }),
    }).isRequired,
    submit: PropTypes.func,
    // translations: PropTypes.shape({}).isRequired,
};

CalendarStyler.defaultProps = {
    submit: undefined,
};

export default (CalendarStyler);
