import PropTypes from 'prop-types';
import React from 'react';
import ReadonlyField from '../../../../design/input/ReadonlyField';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

/**
 * ReadonlyField text (with label and optional tooltip)
 */
function DynamicReadonlyField(
    {
        id,
        dataType,
        ...props
    },
) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    return React.useMemo(() => (
        <DynamicValidationManager id={id}>
            <ReadonlyField
                id={id}
                {...props}
                value={value}
            />
        </DynamicValidationManager>
    ), [value, setData]);
}

DynamicReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
    dataType: PropTypes.string,
};

DynamicReadonlyField.defaultProps = {
    dataType: undefined,
};

export default DynamicReadonlyField;
