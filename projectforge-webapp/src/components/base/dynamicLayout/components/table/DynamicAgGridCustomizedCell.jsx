import React, { Suspense, lazy } from 'react';
import PropTypes from 'prop-types';

// Lazy load customized components to avoid circular dependencies
const CustomizedImageDataPreview = lazy(() => import('../customized/components/ImageDataPreview'));
const CustomizedAddressPhoneNumbers = lazy(() => import('../customized/components/CustomizedAddressPhoneNumbers'));

/**
 * Custom AG Grid cell renderer for customized fields.
 * This component handles special field types that require custom rendering.
 *
 * Note: We use React.lazy to load customized components dynamically
 * to prevent circular dependencies with DynamicAgGrid.
 */
function DynamicAgGridCustomizedCell(props) {
    const {
        colDef,
        data,
    } = props;
    const { field } = colDef;

    // Dispatch to the appropriate customized component based on field ID
    let Component;
    switch (field) {
        case 'address.imagePreview':
            Component = CustomizedImageDataPreview;
            break;
        case 'address.phoneNumbers':
            Component = CustomizedAddressPhoneNumbers;
            break;
        default:
            return <span>{`Customized field '${field}' not supported in AG Grid!`}</span>;
    }

    return (
        <Suspense fallback={<span>...</span>}>
            <Component data={data} />
        </Suspense>
    );
}

DynamicAgGridCustomizedCell.propTypes = {
    colDef: PropTypes.shape({
        field: PropTypes.string.isRequired,
    }).isRequired,
    // eslint-disable-next-line react/forbid-prop-types
    data: PropTypes.any.isRequired,
};

export default DynamicAgGridCustomizedCell;
