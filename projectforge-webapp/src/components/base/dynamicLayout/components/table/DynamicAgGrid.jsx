import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React, { useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { DynamicLayoutContext } from '../../context';
import Formatter from '../../../Formatter';
import 'ag-grid-community/dist/styles/ag-grid.css';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';
import history from '../../../../../utilities/history';
import { getServiceURL } from '../../../../../utilities/rest';

function DynamicAgGrid({
    columnDefs,
    id,
    sortModel,
    rowSelection,
    rowMultiSelectWithClick,
    rowClickRedirectUrl,
    onColumnStatesChangedUrl,
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
        params.columnApi.applyColumnState({
            state: sortModel,
            defaultState: { sort: null },
        });
        if (onGridApiReady) {
            onGridApiReady(params.api, params.columnApi);
        }
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

    const modifyRedirectUrl = (redirectUrl, clickedId) => {
        if (redirectUrl.includes('{id}')) {
            return redirectUrl.replace('{id}', clickedId);
        }
        return redirectUrl.replace('id', clickedId);
    };

    const onRowClicked = (event) => {
        if (!rowClickRedirectUrl) {
            // Do nothing
            return;
        }
        history.push(modifyRedirectUrl(rowClickRedirectUrl, event.data.id));
    };

    const onSelectionChanged = () => {
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
        history.push(modifyRedirectUrl(rowClickRedirectUrl));
    };

    const postColumnStates = (event) => {
        if (onColumnStatesChangedUrl) {
            const columnState = event.columnApi.getColumnState();
            fetch(
                getServiceURL(onColumnStatesChangedUrl), {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(columnState),
                },
            );
        }
    };

    const postColumnStatesDebounced = AwesomeDebouncePromise(postColumnStates, 500);

    const onSortChanged = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const onColumnResized = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const onColumnMoved = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const [components] = useState({
        formatter: Formatter,
    });

    const usedGetRowClass = React.useCallback((params) => {
        const myClass = getRowClassFunction(params);
        if (data?.highlightRowId && params.node.data?.id === data?.highlightRowId) {
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
                onSortChanged={onSortChanged}
                onColumnMoved={onColumnMoved}
                onColumnResized={onColumnResized}
                onRowClicked={onRowClicked}
                pagination={pagination}
                paginationPageSize={paginationPageSize}
                rowClass={rowClass}
                getRowClass={usedGetRowClass}
                accentedSort
            />
        </div>
    ),
    [
        entries,
        ui,
        sortModel,
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
    sortModel: PropTypes.arrayOf(PropTypes.shape({
        colId: PropTypes.string.isRequired,
        sort: PropTypes.string,
        sortIndex: PropTypes.number,
    })),
    rowSelection: PropTypes.string,
    rowMultiSelectWithClick: PropTypes.bool,
    rowClickRedirectUrl: PropTypes.string,
    onColumnStatesChangedUrl: PropTypes.string,
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
