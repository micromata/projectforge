import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import PropTypes from 'prop-types';

/**
 * Custom AG Grid cell renderer for import status column.
 * Shows status text with an error icon if errors are present.
 * The error tooltip is handled by AG Grid's tooltipField property.
 */
function ImportStatusCell({ value, data }) {
    const hasError = data.hasError === true;

    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5em' }}>
            <span>{value}</span>
            {hasError && (
                <FontAwesomeIcon
                    icon={faExclamationTriangle}
                    style={{ color: '#dc3545' }}
                    title="Error"
                />
            )}
        </div>
    );
}

ImportStatusCell.propTypes = {
    value: PropTypes.string,
    data: PropTypes.shape({
        hasError: PropTypes.bool,
    }),
};

export default ImportStatusCell;
