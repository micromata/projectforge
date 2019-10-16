import PropTypes from 'prop-types';
import React from 'react';
import { useClickOutsideHandler } from '../../../../utilities/hooks';
import style from './CalendarInput.module.scss';
import FormattedDateTime from './FormattedDateTime';
import TimeInput from './TimeInput';

function FormattedTimeRange(
    {
        children,
        childrenAsPrefix,
        from,
        id,
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
        <React.Fragment>
            {childrenAsPrefix && children}
            {fromInEditMode
                ? (
                    <div ref={fromEditRef} className={style.editMode}>
                        <TimeInput
                            setTime={setFrom}
                            time={from}
                            id={`time-input-${id}-from`}
                            showDate
                        />
                    </div>
                )
                : (
                    <FormattedDateTime
                        slot="FROM"
                        date={from}
                        onClick={handleFromClick}
                    />
                )}
            {' - '}
            {toInEditMode
                ? (
                    <div ref={toEditRef} className={style.editMode}>
                        <TimeInput
                            setTime={setTo}
                            time={to}
                            id={`time-input-${id}-to`}
                            showDate
                        />
                    </div>
                )
                : (
                    <FormattedDateTime
                        slot="TO"
                        date={to}
                        onClick={handleToClick}
                    />
                )
            }
            {!childrenAsPrefix && children}
        </React.Fragment>
    );
}

FormattedTimeRange.propTypes = {
    from: PropTypes.instanceOf(Date).isRequired,
    id: PropTypes.string.isRequired,
    to: PropTypes.instanceOf(Date).isRequired,
    children: PropTypes.node,
    childrenAsPrefix: PropTypes.bool,
    setFrom: PropTypes.func,
    setTo: PropTypes.func,
};

FormattedTimeRange.defaultProps = {
    children: undefined,
    childrenAsPrefix: false,
    setFrom: undefined,
    setTo: undefined,
};

export default FormattedTimeRange;
