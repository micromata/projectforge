import PropTypes from 'prop-types';
import React from 'react';
import { useClickOutsideHandler } from '../../../../utilities/hooks';
import FormattedDateTime from './FormattedDateTime';

function FormattedTimeRange(
    {
        children,
        from,
        setFrom,
        setTo,
        to,
    },
) {
    const [fromInEditMode, setFromInEditMode] = React.useState(false);
    const [toInEditMode, setToInEditMode] = React.useState(false);

    const fromEditRef = React.useRef(null);
    const toEditRef = React.useRef(null);

    const handleFromClick = () => {
        if (setFrom) {
            setFromInEditMode(true);
        }
    };
    const handleToClick = () => {
        if (setTo) {
            setToInEditMode(true);
        }
    };

    useClickOutsideHandler(fromEditRef, () => setFromInEditMode(false), fromInEditMode);
    useClickOutsideHandler(toEditRef, () => setToInEditMode(false), toInEditMode);

    return (
        <>
            {children}
            <FormattedDateTime
                slot="FROM"
                date={from}
                onClick={handleFromClick}
            />
            -
            <FormattedDateTime
                slot="TO"
                date={to}
                onClick={handleToClick}
            />
        </>
    );
}

FormattedTimeRange.propTypes = {
    from: PropTypes.instanceOf(Date).isRequired,
    to: PropTypes.instanceOf(Date).isRequired,
    children: PropTypes.node,
    setFrom: PropTypes.func,
    setTo: PropTypes.func,
};

FormattedTimeRange.defaultProps = {
    children: undefined,
    setFrom: undefined,
    setTo: undefined,
};

export default FormattedTimeRange;
