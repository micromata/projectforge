import PropTypes from 'prop-types';
import React from 'react';
import DynamicUserSelect from '../select/DynamicUserSelect';
import DynamicTaskSelect from '../select/task';
import DynamicDateInput from './DynamicDateInput';
import DynamicInput from './DynamicInput';
import DynamicTimestampInput from './DynamicTimestampInput';

// All types of 'INPUT' will be resolved here.
function DynamicInputResolver({ dataType, ...props }) {
    let Tag;

    switch (dataType) {
        case 'STRING':
            Tag = DynamicInput;
            break;
        case 'DATE':
            Tag = DynamicDateInput;
            break;
        case 'TIMESTAMP':
            Tag = DynamicTimestampInput;
            break;
        case 'TASK':
            Tag = DynamicTaskSelect;
            break;
        case 'USER':
            Tag = DynamicUserSelect;
            break;
        default:
            return <span>{`${dataType} Input is not implemented.`}</span>;
    }

    return (
        <Tag {...props} />
    );
}

DynamicInputResolver.propTypes = {
    dataType: PropTypes.oneOf([
        // All dataTypes yet implemented for type 'INPUT'.
        'STRING',
        'DATE',
        'TIMESTAMP',
        'TASK',
        'USER',
    ]).isRequired,
};

DynamicInputResolver.defaultProps = {};

export default DynamicInputResolver;
