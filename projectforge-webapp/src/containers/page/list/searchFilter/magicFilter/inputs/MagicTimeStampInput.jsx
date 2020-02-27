import PropTypes from 'prop-types';
import React from 'react';
import DateTimeRange from '../../../../../../components/design/input/calendar/DateTimeRange';
import FormattedTimeRange
    from '../../../../../../components/design/input/calendar/FormattedTimeRange';

function MagicTimeStampInput(
    {
        id,
        onChange,
        selectors,
        value,
    },
) {
    const fragment = <React.Fragment />;

    if (value.to === undefined || value.from === undefined) {
        onChange({
            from: null,
            to: null,
        });
        return fragment;
    }

    if (typeof value.from === 'string' || typeof value.to === 'string') {
        onChange({
            from: new Date(value.from),
            to: new Date(value.to),
        });
        return fragment;
    }

    // TODO CHECK IF FROM IS AFTER TO (AND VICE VERSA)
    const setFrom = from => onChange({
        ...value,
        from,
    });

    const setTo = to => onChange({
        ...value,
        to,
    });

    return (
        <div style={{ width: 700 }}>
            <DateTimeRange
                id={id}
                onChange={onChange}
                {...value}
                setFrom={setFrom}
                setTo={setTo}
                selectors={selectors}
            />
        </div>
    );
}

const dateType = PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.instanceOf(Date),
]);

MagicTimeStampInput.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    selectors: PropTypes.arrayOf(PropTypes.string).isRequired,
    value: PropTypes.shape({
        from: dateType,
        to: dateType,
    }).isRequired,
};

MagicTimeStampInput.defaultProps = {};

MagicTimeStampInput.isEmpty = ({ from, to }) => !(from || to);

MagicTimeStampInput.getLabel = (label, { from, to }, { id }) => {
    if (from && to && typeof from !== 'string' && typeof to !== 'string') {
        return (
            <FormattedTimeRange
                from={from}
                to={to}
            >
                {`${label}: `}
            </FormattedTimeRange>
        );
    }

    return label;
};

export default MagicTimeStampInput;
