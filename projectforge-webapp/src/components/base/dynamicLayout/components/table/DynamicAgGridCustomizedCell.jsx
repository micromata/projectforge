import React from 'react';
import PropTypes from 'prop-types';
import history from '../../../../../utilities/history';

/**
 * Custom AG Grid cell renderer for customized fields: an icon button with an onClick handler.
 */
function DynamicAgGridCustomizedCell(props) {
    const {
        colDef,
        data,
    } = props;
    const { field, cellRendererParams } = colDef;

    // Handle icon button with onClick
    if (cellRendererParams?.icon && cellRendererParams?.onClick) {
        const handleClick = (e) => {
            e.stopPropagation(); // Prevent row click
            e.preventDefault(); // Prevent default action

            // Execute the onClick function with history context
            // The onClick string can be either a function body or a full function
            const onClick = cellRendererParams.onClick.trim();

            // If it starts with "function", evaluate it directly
            if (onClick.startsWith('function')) {
                // eslint-disable-next-line no-new-func
                const clickHandler = Function(`return (${onClick})`)();
                clickHandler(data, history);
            } else {
                // Otherwise, treat it as a function body with data and history parameters
                // eslint-disable-next-line no-new-func
                const clickHandler = Function('data', 'history', onClick);
                clickHandler(data, history);
            }
        };

        return (
            <button
                type="button"
                onClick={handleClick}
                className="btn btn-sm btn-link p-0"
                title={cellRendererParams.tooltip || ''}
                style={{ fontSize: '1.2em' }}
                aria-label={cellRendererParams.tooltip || 'Action'}
            >
                <i className={`fas fa-${cellRendererParams.icon}`} />
            </button>
        );
    }

    return <span>{`Customized field '${field}' not supported in AG Grid!`}</span>;
}

DynamicAgGridCustomizedCell.propTypes = {
    colDef: PropTypes.shape({
        field: PropTypes.string.isRequired,
        cellRendererParams: PropTypes.shape({
            icon: PropTypes.string,
            tooltip: PropTypes.string,
            onClick: PropTypes.string,
        }),
    }).isRequired,
    // eslint-disable-next-line react/forbid-prop-types
    data: PropTypes.any.isRequired,
};

export default DynamicAgGridCustomizedCell;
