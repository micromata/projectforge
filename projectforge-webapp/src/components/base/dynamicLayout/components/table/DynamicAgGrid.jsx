import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React, { useCallback, useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/dist/styles/ag-grid.css';
import { LicenseManager } from 'ag-grid-enterprise';
import 'ag-grid-community/dist/styles/ag-theme-alpine.css';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../context';
import Formatter from '../../../Formatter';
import history from '../../../../../utilities/history';
import { getServiceURL } from '../../../../../utilities/rest';
import { AG_GRID_LOCALE_DE } from './agGridLocalization_de';
import formatterFormat from '../../../FormatterFormat';

LicenseManager.setLicenseKey('CompanyName=Micromata GmbH,LicensedApplication=ProjectForge,LicenseType=SingleApplication,LicensedConcurrentDeveloperCount=2,LicensedProductionInstancesCount=0,AssetReference=AG-029226,SupportServicesEnd=15_July_2023_[v2]_MTY4OTM3NTYwMDAwMA==d9c94f1e7c14ede1402f6577965ae36f');

function DynamicAgGrid(props) {
    const {
        columnDefs,
        id, // If given, data.id is used as entries
        entries, // own entries (not data.id)
        sortModel,
        rowSelection,
        rowMultiSelectWithClick,
        rowClickRedirectUrl,
        rowClickFunction,
        onCellClicked,
        onColumnStatesChangedUrl,
        onGridApiReady,
        pagination,
        paginationPageSize,
        getRowClass,
        suppressRowClickSelection,
        components,
        // If not usable from locale from authentication, e. g. in public pages:
        locale,
        dateFormat,
        thousandSeparator,
        decimalSeparator,
        timestampFormatSeconds,
        timestampFormatMinutes,
        currency,
        // By authentication object:
        userLocale,
        userDateFormat,
        userThousandSeparator,
        userDecimalSeparator,
        userTimestampFormatSeconds,
        userTimestampFormatMinutes,
        userCurrency,
        height,
        highlightId,
    } = props;
    // eslint-disable-next-line no-new-func
    const getRowClassFunction = Function('params', getRowClass);
    const rowClass = 'ag-row-standard';
    const { data, ui, variables } = React.useContext(DynamicLayoutContext);
    const [gridApi, setGridApi] = useState();
    const [columnApi, setColumnApi] = useState();
    const gridRef = useRef();
    // const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const rowData = entries || Object.getByString(data, id) || Object.getByString(variables, id) || '';
    const { selectedEntityIds } = data;
    /*
    const showHighlightedRow = () => {
        console.log('showHighlightedRow');
        const highlightRowId = data.highlightRowId || highlightId;
        if (gridApi && visible && highlightRowId) {
            let highlightIndex;
            gridApi.forEachNode((rowNode, index) => {
                if (!highlightIndex && rowNode.data?.id === highlightRowId) {
                    highlightIndex = index;
                }
            });
            if (highlightIndex) {
                console.log('scroll to', highlightIndex);
                gridApi.ensureIndexVisible(highlightIndex);
            }
        }
    }; */

    const onGridReady = React.useCallback((params) => {
        setGridApi(params.api);
        setColumnApi(params.columnApi);
        params.columnApi.applyColumnState({
            state: sortModel,
            defaultState: { sort: null },
        });
        if (onGridApiReady) {
            onGridApiReady(params.api, params.columnApi);
        }
        if (!height) {
            params.api.setDomLayout('autoHeight'); // Needed to get maximum height.
        }
        // showHighlightedRow();
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
        if (gridApi && (data.highlightRowId || highlightId)) {
            // Force redraw to highlight the current row (otherwise getRawClass is not updated).
            gridApi.redrawRows();
        }
    }, [gridApi, data.highlightRowId, highlightId]);

    /*
    React.useEffect(() => {
        showHighlightedRow();
    }, [gridApi, data.highlightRowId, highlightId, visible]);
    */

    const getLocaleText = (params) => {
        const { defaultValue, key } = params;
        if (key === 'dateFormat') {
            return dateFormat || AG_GRID_LOCALE_DE[key] || defaultValue;
        }
        if (key === 'thousandSeparator') {
            return thousandSeparator || userThousandSeparator
                || AG_GRID_LOCALE_DE[key] || defaultValue;
        }
        if (key === 'decimalSeparator') {
            return decimalSeparator || userDecimalSeparator
                || AG_GRID_LOCALE_DE[key] || defaultValue;
        }
        if ((locale || userLocale) === 'de') return AG_GRID_LOCALE_DE[key] || defaultValue;
        return params.defaultValue;
    };

    const modifyRedirectUrl = (redirectUrl, clickedId) => {
        if (redirectUrl.includes('{id}')) {
            return redirectUrl.replace('{id}', clickedId);
        }
        return redirectUrl.replace('id', clickedId);
    };

    const onRowClicked = (event) => {
        if (rowClickFunction) {
            rowClickFunction(event);
            return;
        }
        if (!rowClickRedirectUrl) {
            // Do nothing
            return;
        }
        history.push(modifyRedirectUrl(rowClickRedirectUrl, event.data.id));
    };

    /*
    const onFirstDataRendered = () => {
        showHighlightedRow();
    }; */

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
            fetch(getServiceURL(onColumnStatesChangedUrl), {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(columnState),
            });
        }
    };

    const postColumnStatesDebounced = AwesomeDebouncePromise(postColumnStates, 500);

    const onSortChanged = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const onColumnResized = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const onColumnVisible = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const onColumnMoved = async (event) => {
        await postColumnStatesDebounced(event);
    };

    // Needed, otherwise the cellRenderer output isn't used and
    // [object] is copied to clipboard.
    const processCellForClipboard = (params) => {
        const colDef = params.column.getColDef();
        if (colDef.valueFormatter) {
            return colDef.valueFormatter({
                ...params,
                data: params.node?.data,
                colDef,
            });
        }
        const { value } = params;
        const { cellRenderer, cellRendererParams } = colDef;
        if (cellRenderer === 'formatter') {
            return formatterFormat(
                value,
                cellRendererParams?.dataType,
                dateFormat || userDateFormat,
                timestampFormatSeconds || userTimestampFormatSeconds,
                timestampFormatMinutes || userTimestampFormatMinutes,
                locale || userLocale,
                currency || userCurrency,
            );
        }
        return value;
    };

    // Isn't used by Excel-Export
    // const processCellCallback = ({ column, value }) => {
    //     console.log(column, value);
    // };

    const [allComponents] = useState({
        formatter: Formatter,
        ...components,
    });

    const usedGetRowClass = React.useCallback((params) => {
        const myClass = getRowClassFunction(params);
        const rowId = highlightId || data?.highlightRowId;
        if (rowId && params.node.data?.id === rowId) {
            const classes = [];
            classes.push('ag-row-highlighted');
            if (myClass) {
                classes.push(myClass);
            }
            return classes;
        }
        return myClass;
    }, [data.highlightRowId, highlightId]);
    return React.useMemo(
        () => (
            <div
                className="ag-theme-alpine"
                style={{ width: '100%', height }}
            >
                <AgGridReact
                    {...props}
                    ref={gridRef}
                    rowData={rowData}
                    components={allComponents}
                    columnDefs={columnDefs}
                    rowSelection={rowSelection}
                    rowMultiSelectWithClick={rowMultiSelectWithClick}
                    onGridReady={onGridReady}
                    onSelectionChanged={onSelectionChanged}
                    onSortChanged={onSortChanged}
                    onColumnMoved={onColumnMoved}
                    onColumnResized={onColumnResized}
                    onColumnVisible={onColumnVisible}
                    onRowClicked={onRowClicked}
                    onCellClicked={onCellClicked}
                    pagination={pagination}
                    paginationPageSize={paginationPageSize}
                    rowClass={rowClass}
                    getRowClass={usedGetRowClass}
                    accentedSort
                    enableRangeSelection
                    suppressRowClickSelection={suppressRowClickSelection}
                    getLocaleText={getLocaleText}
                    processCellForClipboard={processCellForClipboard}
                    // processCellCallback={processCellCallback}
                    tooltipShowDelay={0}
                    suppressScrollOnNewData
                    // onFirstDataRendered={onFirstDataRendered}
                />
            </div>
        ),
        [
            rowData,
            ui,
            sortModel,
            data.highlightRowId,
            // gridStyle,
            columnDefs,
            rowSelection,
            rowMultiSelectWithClick,
            usedGetRowClass,
            onGridReady,
        ],
    );
}

DynamicAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        field: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    id: PropTypes.string,
    entries: PropTypes.arrayOf(PropTypes.shape()),
    sortModel: PropTypes.arrayOf(PropTypes.shape({
        colId: PropTypes.string.isRequired,
        sort: PropTypes.string,
        sortIndex: PropTypes.number,
    })),
    rowSelection: PropTypes.string,
    rowMultiSelectWithClick: PropTypes.bool,
    rowClickRedirectUrl: PropTypes.string,
    rowClickFunction: PropTypes.func,
    onColumnStatesChangedUrl: PropTypes.string,
    pagination: PropTypes.bool,
    paginationPageSize: PropTypes.number,
    getRowClass: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.string,
    ]),
    suppressRowClickSelection: PropTypes.bool,
    checkboxSelection: PropTypes.bool,
    components: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.any,
    ]),
    locale: PropTypes.string,
    dateFormat: PropTypes.string,
    thousandSeparator: PropTypes.string,
    decimalSeparator: PropTypes.string,
    timestampFormatSeconds: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
    currency: PropTypes.string,
    height: PropTypes.number,
    // visible: PropTypes.bool,
};

DynamicAgGrid.defaultProps = {
    id: undefined,
    entries: undefined,
    pagination: undefined,
    paginationPageSize: undefined,
    getRowClass: undefined,
    rowClickRedirectUrl: undefined,
    onRowClicked: undefined,
    suppressRowClickSelection: undefined,
    components: undefined,
    locale: undefined,
    dateFormat: 'YYYY-MM-dd',
    thousandSeparator: ',',
    decimalSeparator: '.',
    timestampFormatSeconds: 'YYYY-MM-dd HH:mm:ss',
    timestampFormatMinutes: 'YYYY-MM-dd HH:mm',
    currency: '€',
    height: undefined,
    // visible: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    userLocale: authentication?.user?.locale,
    userDateFormat: authentication?.user?.dateFormat,
    userThousandSeparator: authentication?.user?.thousandSeparator,
    userDecimalSeparator: authentication?.user?.decimalSeparator,
    userTimestampFormatSeconds: authentication?.user?.timestampFormatSeconds,
    userTimestampFormatMinutes: authentication?.user?.timestampFormatMinutes,
    userCurrency: authentication?.user?.currency,
});

export default connect(mapStateToProps)(DynamicAgGrid);
