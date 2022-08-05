import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { CardFooter } from 'reactstrap';
import styles from './CalendarEventTooltip.module.scss';
import { CardHeader, CardBody } from '../../../components/design';

function CalendarEventTooltip(props) {
    const { forwardRef, event } = props;
    const extendedProps = event?.extendedProps;
    const tooltip = extendedProps?.tooltip;

    return (
        <div
            ref={forwardRef}
            className={classNames('card', styles.eventTooltip, !event && styles.hidden)}
        >
            {tooltip?.title && (
                <CardHeader>
                    <b>{tooltip?.title}</b>
                </CardHeader>
            ) }
            <CardBody>
                <div
                    dangerouslySetInnerHTML={{ __html: tooltip?.text }}
                />
            </CardBody>
            {extendedProps?.duration && (
                <CardFooter>
                    {extendedProps?.duration}
                </CardFooter>
            )}
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
    event: PropTypes.shape({
        extendedProps: PropTypes.arrayOf(PropTypes.shape({})),
    }),
};

CalendarEventTooltip.defaultProps = {
    event: null,
};

export default CalendarEventTooltip;
