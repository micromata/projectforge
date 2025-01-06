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
        value,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const dataValue = value || Object.getByString(data, id) || '';

    return React.useMemo(() => (
        <DynamicValidationManager id={id}>
            <ReadonlyField
                id={`${ui.uid}-${id}`}
                {...props}
                value={dataValue}
                dataType={dataType}
            />
        </DynamicValidationManager>
    ), [dataValue, setData, id, dataType, props]);
}

DynamicReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
    dataType: PropTypes.string,
};

export default DynamicReadonlyField;
