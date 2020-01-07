import PropTypes from 'prop-types';
import React from 'react';
import CalendarStyler from '../../../../../panel/calendar/CalendarStyler';

function MagicColorPicker({ onSubmit, ...props }) {
    return (
        <CalendarStyler calendar={props} submit={onSubmit} />
    );
}

MagicColorPicker.propTypes = {
    onSubmit: PropTypes.func.isRequired,
};

MagicColorPicker.defaultProps = {};

MagicColorPicker.getLabel = label => label;

export default MagicColorPicker;
