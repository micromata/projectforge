import PropTypes from 'prop-types';
import React from 'react';
import DynamicObjectSelect from '../select/DynamicObjectSelect';
import DynamicTaskSelect from '../select/task';
import DynamicAutoCompletion from './DynamicAutoCompletion';
import DynamicDateInput from './DynamicDateInput';
import DynamicInput from './DynamicInput';
import DynamicTimeInput from './DynamicTimeInput';
import DynamicTimestampInput from './DynamicTimestampInput';

// All types of 'INPUT' will be resolved here.
function DynamicInputResolver(
    {
        dataType,
        autoCompletionUrl,
        autoCompletionUrlParams,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        type,
        ...props
    },
) {
    let Tag;
    const additionalProps = {
        url: autoCompletionUrl,
        urlparams: autoCompletionUrlParams,
    };

    switch (dataType) {
        case 'STRING':
            if (autoCompletionUrl) {
                Tag = DynamicAutoCompletion;
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
        case 'GROUP':
        case 'EMPLOYEE':
        case 'COST1':
            Tag = DynamicObjectSelect;
            additionalProps.type = dataType;
            break;
        case 'COST2':
            Tag = DynamicObjectSelect;
            additionalProps.type = dataType;
            break;
        case 'KONTO':
            Tag = DynamicObjectSelect;
            additionalProps.type = dataType;
            break;
        case 'INT':
        case 'LONG':
        case 'DECIMAL':
        case 'NUMBER':
            Tag = DynamicInput;
            additionalProps.type = 'number';
            break;
        case 'PASSWORD':
            Tag = DynamicInput;
            additionalProps.type = 'password';
            break;
        case 'TIME':
            Tag = DynamicTimeInput;
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
        'EMPLOYEE',
        'COST1',
        'COST2',
        'TIMESTAMP',
        'TASK',
        'USER',
        'INT',
        'LONG',
        'KONTO',
        'DECIMAL',
        'NUMBER',
        'PASSWORD',
        'TIME',
    ]).isRequired,
    type: PropTypes.string.isRequired,
    autoCompletionUrl: PropTypes.string,
    autoCompletionUrlParams: PropTypes.shape({}),
};

export default DynamicInputResolver;
