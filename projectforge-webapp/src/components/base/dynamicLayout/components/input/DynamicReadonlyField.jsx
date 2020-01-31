import PropTypes from 'prop-types';
import React from 'react';
import ReadonlyField from '../../../../design/input/ReadonlyField';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

/**
 * ReadonlyField text (with label and optional toolip)
 */
function DynamicReadonlyField({ id, focus, ...props }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    return React.useMemo(() => {

        return (
            <DynamicValidationManager id={id}>
                <ReadonlyField
                    id={id}
                    {...props}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [value, setData]);
}

DynamicReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
};

DynamicReadonlyField.defaultProps = {
};

export default DynamicReadonlyField;
