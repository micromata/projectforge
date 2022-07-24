import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { CardFooter } from 'reactstrap';
import styles from './CalendarEventTooltip.module.scss';
import { Card, CardHeader, CardBody } from '../../../components/design';

function CalendarEventTooltip(props) {
    const { forwardRef, event } = props;
    const tooltip = event?.extendedProps?.tooltip;

    return (
        <div
            ref={forwardRef}
            className={classNames(styles.eventTooltip, !event && styles.hidden)}
        >
            <Card>
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
                {tooltip?.formattedDuration && (
                    <CardFooter>
                        {tooltip?.formattedDuration}
                    </CardFooter>
                )}
            </Card>
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
        PropTypes.shape({}),
        null,
    ]),
};

CalendarEventTooltip.defaultProps = {
    event: null,
};

export default CalendarEventTooltip;
