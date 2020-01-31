import PropTypes from 'prop-types';
import React from 'react';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicValidationManager({ children, id }) {
    const { validationErrors } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        const customValidation = Array.findByField(validationErrors, 'fieldId', id);

        return (
            <ValidationManager customValidation={customValidation}>
                {children}
            </ValidationManager>
        );
    }, [validationErrors, children]);
}

DynamicValidationManager.propTypes = {
    id: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
};

DynamicValidationManager.defaultProps = {};

export default DynamicValidationManager;
