import PropTypes from 'prop-types';
import React from 'react';
import DateTimeRange from '../../../../../../components/design/input/calendar/DateTimeRange';
import FormattedTimeRange
    from '../../../../../../components/design/input/calendar/FormattedTimeRange';

function MagicTimeStampInput(
    {
        filterType,
        id,
        onChange,
        selectors,
        translations,
        value,
    },
) {
    const fragment = <></>;

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
    const setFrom = (from) => onChange({
        ...value,
        from,
    });

    const setTo = (to) => onChange({
        ...value,
        to,
    });

    return (
        <div style={{ width: 700 }}>
            <DateTimeRange
                hideTimeInput={filterType === 'DATE'}
                id={id}
                onChange={onChange}
                {...value}
                setFrom={setFrom}
                setTo={setTo}
                selectors={selectors}
                translations={translations}
            />
        </div>
    );
}

const dateType = PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.instanceOf(Date),
]);

MagicTimeStampInput.propTypes = {
    filterType: PropTypes.oneOf(['TIMESTAMP', 'DATE']).isRequired,
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({
        from: dateType,
        to: dateType,
    }).isRequired,
    selectors: PropTypes.arrayOf(PropTypes.string),
    translations: PropTypes.shape({}),
};

MagicTimeStampInput.defaultProps = {
    selectors: undefined,
    translations: {},
};

MagicTimeStampInput.isEmpty = ({ from, to }) => !(from || to);

MagicTimeStampInput.getLabel = (label, { from, to }) => {
    if (from && to) {
        return (
            <FormattedTimeRange
                from={typeof from === 'string' ? new Date(from) : from}
                to={typeof to === 'string' ? new Date(to) : to}
            >
                {`${label}: `}
            </FormattedTimeRange>
        );
    }

    return label;
};

export default MagicTimeStampInput;
