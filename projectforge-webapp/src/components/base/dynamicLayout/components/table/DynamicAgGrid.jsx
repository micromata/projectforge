import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';

function DynamicAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
    onGridApiReady,
    pagination,
    paginationPageSize,
    getRowClass,
}) {
    // eslint-disable-next-line no-new-func
    const getRowClassFunction = Function('params', getRowClass);
    const rowClass = 'ag-row-standard';
    const { data, ui } = React.useContext(DynamicLayoutContext);
    const [gridApi, setGridApi] = useState();

    const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const entries = Object.getByString(data, id) || '';
    const { selectedEntityIds } = data;

    const onGridReady = React.useCallback((params) => {
        setGridApi(params.api);
        onGridApiReady(params.api);
        params.api.setDomLayout('autoHeight'); // Needed to get maximum height.
    }, [selectedEntityIds, setGridApi]);

    React.useEffect(() => {
        if (gridApi && selectedEntityIds) {
            gridApi.forEachNode((node) => {
                const row = node.data;
                // Recover previous selected nodes from server (if any):
                node.setSelected(selectedEntityIds.includes(row.id));
            });
        }
    }, [gridApi, selectedEntityIds]);

    return React.useMemo(() => (
        <div
            className="ag-theme-alpine"
            style={gridStyle}
        >
            <AgGridReact
                rowData={entries}
                columnDefs={columnDefs}
                rowSelection={rowSelection}
                rowMultiSelectWithClick={rowMultiSelectWithClick}
                onGridReady={onGridReady}
                pagination={pagination}
                paginationPageSize={paginationPageSize}
                rowClass={rowClass}
                getRowClass={getRowClassFunction}
            />
        </div>
    ),
    [
        entries,
        ui,
        gridStyle,
        columnDefs,
        rowSelection,
        rowMultiSelectWithClick,
        onGridReady,
    ]);
}

DynamicAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    id: PropTypes.string,
    rowSelection: PropTypes.string,
    rowMultiSelectWithClick: PropTypes.bool,
    pagination: PropTypes.bool,
    paginationPageSize: PropTypes.number,
    getRowClass: PropTypes.shape({}),
};

DynamicAgGrid.defaultProps = {
    id: undefined,
    pagination: undefined,
    paginationPageSize: undefined,
    getRowClass: undefined,
};

export default DynamicAgGrid;
