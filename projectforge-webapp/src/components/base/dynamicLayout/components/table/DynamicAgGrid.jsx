import PropTypes from 'prop-types';
import React, { useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';
import Formatter from '../../../Formatter';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';
import history from '../../../../../utilities/history';
// import {getServiceURL, handleHTTPErrors} from "../../../../../utilities/rest";

function DynamicAgGrid({
    columnDefs,
    id,
    rowSelection,
    rowMultiSelectWithClick,
    rowClickRedirectUrl,
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
    const gridRef = useRef();
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

    React.useEffect(() => {
        if (gridApi && data.highlightRowId) {
            // Force redraw to highlight the current row (otherwise getRawClass is not updated).
            gridApi.redrawRows();
        }
    }, [gridApi, data.highlightRowId]);

    const onSelectionChanged = React.useCallback(() => {
        if (!rowClickRedirectUrl) {
            // Do nothing
            return;
        }
        const selectedRows = gridRef.current.api.getSelectedRows();
        if (!selectedRows || selectedRows.size === 0) {
            // No row(s) selected.
            return;
        }
        const firstSelectedRowId = selectedRows[0].id;
        if (!firstSelectedRowId) {
            // Can't detect id.
            return;
        }
        history.push(rowClickRedirectUrl.replace('id', firstSelectedRowId));
        /* handle rowClickPostUrl
        fetch(
            getServiceURL(`${rowClickPostUrl}/${row.id}`),
            {
                credentials: 'include',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                },
                body: JSON.stringify({
                    data,
                }),
            },
        )
            .then(handleHTTPErrors)
            .then((body) => body.json())
            .then((json) => {
                callAction({ responseAction: json });
            })
            // eslint-disable-next-line no-alert
            .catch((error) => alert(`Internal error: ${error}`)); */
    }, [gridApi]);

    const [components] = useState({
        formatter: Formatter,
    });

    const usedGetRowClass = React.useCallback((params) => {
        const myClass = getRowClassFunction(params);
        if (params.node.data?.id === data?.highlightRowId) {
            const classes = [];
            classes.push('ag-row-highlighted');
            if (myClass) {
                classes.push(myClass);
            }
            return classes;
        }
        return myClass;
    }, [data.highlightRowId]);

    return React.useMemo(() => (
        <div
            className="ag-theme-alpine"
            style={gridStyle}
        >
            <AgGridReact
                ref={gridRef}
                rowData={entries}
                components={components}
                columnDefs={columnDefs}
                rowSelection={rowSelection}
                rowMultiSelectWithClick={rowMultiSelectWithClick}
                onGridReady={onGridReady}
                onSelectionChanged={onSelectionChanged}
                pagination={pagination}
                paginationPageSize={paginationPageSize}
                rowClass={rowClass}
                getRowClass={usedGetRowClass}
            />
        </div>
    ),
    [
        entries,
        ui,
        data.highlightRowId,
        gridStyle,
        columnDefs,
        rowSelection,
        rowMultiSelectWithClick,
        usedGetRowClass,
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
    rowClickRedirectUrl: PropTypes.string,
    pagination: PropTypes.bool,
    paginationPageSize: PropTypes.number,
    getRowClass: PropTypes.shape({}),
};

DynamicAgGrid.defaultProps = {
    id: undefined,
    pagination: undefined,
    paginationPageSize: undefined,
    getRowClass: undefined,
    rowClickRedirectUrl: undefined,
};

export default DynamicAgGrid;
