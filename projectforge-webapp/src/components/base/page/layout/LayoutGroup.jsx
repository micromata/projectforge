import PropTypes from 'prop-types';
import React from 'react';

function LayoutGroup() {
    return <span style={{ color: 'red' }}>Don&rsquo;t use the LayoutGroup anymore.</span>;
}

LayoutGroup.propTypes = {
    changeDataField: PropTypes.func,
    // PropType validation with type array has to be allowed here.
    // Otherwise it will create an endless loop of groups.
    /* eslint-disable-next-line react/forbid-prop-types */
    content: PropTypes.array,
    data: PropTypes.shape({}),
    variables: PropTypes.shape({}),
    length: PropTypes.number,
    title: PropTypes.string,
    type: PropTypes.string,
    validation: PropTypes.shape({}),
    translations: PropTypes.shape({}),
};

LayoutGroup.defaultProps = {
    changeDataField: undefined,
    content: [],
    data: {},
    variables: {},
    length: undefined,
    title: undefined,
    type: 'CONTAINER',
    validation: {},
    translations: {},
};

export default LayoutGroup;
