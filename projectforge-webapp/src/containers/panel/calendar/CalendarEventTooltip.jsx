import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import styles from './CalendarEventTooltip.module.scss';

function CalendarEventTooltip(props) {
    const { forwardRef, event } = props;

    return (
        <div ref={forwardRef} className={classNames(styles.eventTooltip, !event && styles.hidden)}>
            <h1>Tooltip</h1>
        </div>
    );
}

CalendarEventTooltip.propTypes = {
    forwardRef: PropTypes.oneOfType([
        // Either a function
        PropTypes.func,
        // Or the instance of a DOM native element (see the note about SSR)
        PropTypes.shape({ current: PropTypes.instanceOf(Element) }),
    ]).isRequired,
    event: PropTypes.oneOfType([
        PropTypes.shape({

        }),
        null,
    ]),
};

CalendarEventTooltip.defaultProps = {
    event: null,
};

export default CalendarEventTooltip;
