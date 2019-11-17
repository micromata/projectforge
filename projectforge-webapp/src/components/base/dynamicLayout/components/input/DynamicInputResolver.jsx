import PropTypes from 'prop-types';
import React from 'react';
import DynamicReactSelect from '../select/DynamicReactSelect';
import DynamicUserSelect from '../select/DynamicUserSelect';
import DynamicTaskSelect from '../select/task';
import DynamicDateInput from './DynamicDateInput';
import DynamicInput from './DynamicInput';
import DynamicTimestampInput from './DynamicTimestampInput';

// All types of 'INPUT' will be resolved here.
function DynamicInputResolver({ dataType, autoCompletionUrl, ...props }) {
    let Tag;
    const additionalProps = {};

    switch (dataType) {
        case 'STRING':
            if (autoCompletionUrl) {
                Tag = DynamicReactSelect;
                additionalProps.autoCompletion = { url: autoCompletionUrl };
            } else {
                Tag = DynamicInput;
            }
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
        case 'INT':
        case 'DECIMAL':
        case 'NUMBER':
            Tag = DynamicInput;
            additionalProps.type = 'number';
            break;
        default:
            return <span>{`${dataType} Input is not implemented.`}</span>;
    }

    return (
        <Tag {...props} {...additionalProps} />
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
        'INT',
        'DECIMAL',
        'NUMBER',
    ]).isRequired,
    autoCompletionUrl: PropTypes.string,
};

DynamicInputResolver.defaultProps = {
    autoCompletionUrl: undefined,
};

export default DynamicInputResolver;
