import PropTypes from 'prop-types';
import React from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';

import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';

function DynamicListPageAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
}) {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    const containerStyle = React.useMemo(() => ({ width: '100%', height: '500px' }), []);
    const gridStyle = React.useMemo(() => ({ height: '100%', width: '100%' }), []);
    const entries = Object.getByString(data, id) || '';

    return React.useMemo(() => (
        <div style={containerStyle}>
            <div
                className="ag-theme-alpine"
                style={gridStyle}
            >
                <AgGridReact
                    rowData={entries}
                    columnDefs={columnDefs}
                    rowSelection={rowSelection}
                    rowMultiSelectWithClick={rowMultiSelectWithClick}
                />
            </div>
        </div>
    ), [entries, ui]);
}

DynamicListPageAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    id: PropTypes.string,
    rowSelection: PropTypes.string,
    rowMultiSelectWithClick: PropTypes.bool,
};

DynamicListPageAgGrid.defaultProps = {
    id: undefined,
};

export default DynamicListPageAgGrid;
