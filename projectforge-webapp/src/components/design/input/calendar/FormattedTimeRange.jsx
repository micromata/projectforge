import PropTypes from 'prop-types';
import React from 'react';
import FormattedDateTime from './FormattedDateTime';

function FormattedTimeRange(
    {
        children,
        childrenAsPrefix,
        from,
        to,
    },
) {
    return (
        <React.Fragment>
            {childrenAsPrefix && children}
            <FormattedDateTime slot="FROM" date={from} />
            {' - '}
            <FormattedDateTime slot="TO" date={to} />
            {!childrenAsPrefix && children}
        </React.Fragment>
    );
}

FormattedTimeRange.propTypes = {
    from: PropTypes.instanceOf(Date).isRequired,
    to: PropTypes.instanceOf(Date).isRequired,
    children: PropTypes.node,
    childrenAsPrefix: PropTypes.bool,
};

FormattedTimeRange.defaultProps = {
    children: undefined,
    childrenAsPrefix: false,
};

export default FormattedTimeRange;
