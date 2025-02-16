import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React, { useMemo, useRef, useEffect, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { LicenseManager, ModuleRegistry, AllEnterpriseModule, themeBalham } from 'ag-grid-enterprise';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../context';
import Formatter from '../../../Formatter';
import history from '../../../../../utilities/history';
import { getServiceURL } from '../../../../../utilities/rest';
import { AG_GRID_LOCALE_DE } from './agGridLocalization_de';
import formatterFormat from '../../../FormatterFormat';
import DynamicAgGridDiffCell from './DynamicAgGridDiffCell';

LicenseManager.setLicenseKey('Using_this_{AG_Grid}_Enterprise_key_{AG-059988}_in_excess_of_the_licence_granted_is_not_permitted___Please_report_misuse_to_legal@ag-grid.com___For_help_with_changing_this_key_please_contact_info@ag-grid.com___{Micromata_GmbH}_is_granted_a_{Single_Application}_Developer_License_for_the_application_{ProjectForge}_only_for_{2}_Front-End_JavaScript_developers___All_Front-End_JavaScript_developers_working_on_{ProjectForge}_need_to_be_licensed___{ProjectForge}_has_not_been_granted_a_Deployment_License_Add-on___This_key_works_with_{AG_Grid}_Enterprise_versions_released_before_{14_July_2025}____[v3]_[01]_MTc1MjQ0NzYwMDAwMA==2c2e5c05a1f3b34a534c11405051440a');
ModuleRegistry.registerModules([AllEnterpriseModule]);

const agTheme = themeBalham
    .withParams({
    });

function DynamicAgGrid(props) {
    const {
        columnDefs,
        selectionColumnDef,
        id, // If given, data.id is used as entries
        entries, // own entries (not data.id)
        sortModel,
        rowSelection,
        rowClickRedirectUrl,
        rowClickFunction,
        rowClickOpenModal,
        onCellClicked,
        onColumnStatesChangedUrl,
        onGridApiReady,
        pagination,
        paginationPageSize,
        paginationPageSizeSelector,
        getRowClass,
        components,
        // If not usable from locale from authentication, e. g. in public pages:
        locale,
        dateFormat = 'YYYY-MM-dd',
        thousandSeparator = ',',
        decimalSeparator = '.',
        timestampFormatSeconds = 'YYYY-MM-dd HH:mm:ss',
        timestampFormatMinutes = 'YYYY-MM-dd HH:mm',
        currency = '€',
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
    const { data, variables } = React.useContext(DynamicLayoutContext);
    const [gridApi, setGridApi] = useState();
    const gridRef = useRef();
    // const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const [processedColumnDefs, setProcessedColumnDefs] = useState([]);
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
        params.api.applyColumnState({
            state: sortModel,
            defaultState: { sort: null },
        });
        if (onGridApiReady) {
            onGridApiReady(params.api, params.columnApi);
        }
        if (!height) {
            // params.api.setDomLayout('autoHeight'); // Needed to get maximum height.
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

    useEffect(() => {
        // Gehe durch alle columnDefs und setze den comparator für agDateColumnFilter
        const updatedColumnDefs = columnDefs.map((col) => {
            if (col.filter === 'agDateColumnFilter') {
                return {
                    ...col,
                    filterParams: {
                        ...col.filterParams,
                        comparator: (filterLocalDateAtMidnight, cellValue) => {
                            if (!cellValue) return -1;

                            // Wandelt "YYYY-MM-DD" in ein JS Date-Objekt um
                            const [year, month, day] = cellValue.split('-');
                            const cellDate = new Date(Number(year), Number(month) - 1, Number(day));

                            if (filterLocalDateAtMidnight.getTime() === cellDate.getTime()) {
                                return 0;
                            }
                            return cellDate < filterLocalDateAtMidnight ? -1 : 1;
                        },
                    },
                };
            }
            return col;
        });

        setProcessedColumnDefs(updatedColumnDefs);
    }, [columnDefs]);

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
        const redirectUrl = modifyRedirectUrl(rowClickRedirectUrl, event.data.id);
        if (rowClickOpenModal) {
            const historyState = { };

            historyState.background = history.location;

            history.push(redirectUrl, historyState);
        } else {
            history.push(redirectUrl);
        }
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
        history.push(modifyRedirectUrl(rowClickRedirectUrl, firstSelectedRowId));
    };

    const postColumnStates = (event) => {
        if (onColumnStatesChangedUrl) {
            const columnState = event.api.getColumnState();
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

    const onColumnPinned = async (event) => {
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

    const allComponents = useMemo(() => ({
        formatter: Formatter,
        diffCell: DynamicAgGridDiffCell,
        ...components,
    }), [components]);

    const usedGetRowClass = React.useCallback((params) => {
        const myClass = getRowClassFunction(params);
        const rowId = highlightId || data?.highlightRowId;
        if (rowId && params.node.data?.id === rowId) {
            const classes = ['ag-row-highlighted'];
            if (myClass) {
                classes.push(myClass);
            }
            return classes;
        }
        return myClass;
    }, [data.highlightRowId, highlightId, getRowClass]);
    return React.useMemo(
        () => (
            <div
                style={{ minWidth: '100%', height }}
            >
                <AgGridReact
                    // Show popup (e.g. for choosing columns) in body, not in grid.
                    // Otherwise, scrolling required.
                    popupParent={document.body}
                    theme={agTheme}
                    ref={gridRef}
                    rowData={rowData}
                    components={allComponents}
                    columnDefs={processedColumnDefs}
                    selectionColumnDef={selectionColumnDef}
                    rowSelection={rowSelection}
                    onGridReady={onGridReady}
                    onSelectionChanged={onSelectionChanged}
                    onSortChanged={onSortChanged}
                    onColumnMoved={onColumnMoved}
                    onColumnResized={onColumnResized}
                    onColumnVisible={onColumnVisible}
                    onColumnPinned={onColumnPinned}
                    onRowClicked={onRowClicked}
                    onCellClicked={onCellClicked}
                    pagination={pagination}
                    paginationPageSize={data.paginationPageSize || paginationPageSize}
                    paginationPageSizeSelector={paginationPageSizeSelector}
                    rowClass={rowClass}
                    getRowClass={usedGetRowClass}
                    suppressHorizontalScroll={false}
                    accentedSort
                    cellSelection
                    getLocaleText={getLocaleText}
                    processCellForClipboard={processCellForClipboard}
                    // processCellCallback={processCellCallback}
                    tooltipShowDelay={0}
                    suppressScrollOnNewData
                    // onFirstDataRendered={onFirstDataRendered}
                    domLayout="autoHeight"
                    sideBar={{
                        toolPanels: [
                            {
                                id: 'columns',
                                labelDefault: 'Choose Columns',
                                labelKey: 'columns',
                                iconKey: 'columns',
                                toolPanel: 'agColumnsToolPanel',
                                toolPanelParams: {
                                    suppressSyncLayoutWithGrid: false,
                                    suppressColumnFilter: false,
                                    suppressColumnSelectAll: false,
                                    suppressColumnExpandAll: false,
                                },
                            },
                        ],
                        defaultToolPanel: null,
                    }}
                />
            </div>
        ),
        [
            processedColumnDefs,
            data,
            selectionColumnDef,
            sortModel,
            rowSelection,
            paginationPageSize,
        ],
    );
}

DynamicAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        field: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    selectionColumnDef: PropTypes.arrayOf(PropTypes.shape({
        pinned: PropTypes.string,
        resizable: PropTypes.bool,
        sortable: PropTypes.bool,
    })),
    id: PropTypes.string,
    entries: PropTypes.arrayOf(PropTypes.shape()),
    sortModel: PropTypes.arrayOf(PropTypes.shape({
        colId: PropTypes.string.isRequired,
        sort: PropTypes.string,
        sortIndex: PropTypes.number,
    })),
    rowSelection: PropTypes.shape({
        mode: PropTypes.string,
        enableClickSelection: PropTypes.bool,
        enableSelectionWithoutKeys: PropTypes.bool,
    }),
    rowClickRedirectUrl: PropTypes.string,
    rowClickOpenModal: PropTypes.bool,
    rowClickFunction: PropTypes.func,
    onColumnStatesChangedUrl: PropTypes.string,
    pagination: PropTypes.bool,
    paginationPageSize: PropTypes.number,
    paginationPageSizeSelector: PropTypes.arrayOf(PropTypes.number),
    getRowClass: PropTypes.oneOfType([
        PropTypes.shape({}),
        PropTypes.string,
    ]),
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
    userLocale: PropTypes.string,
    onCellClicked: PropTypes.func,
    onGridApiReady: PropTypes.func,
    userDateFormat: PropTypes.string,
    userThousandSeparator: PropTypes.string,
    userDecimalSeparator: PropTypes.string,
    userTimestampFormatSeconds: PropTypes.string,
    userTimestampFormatMinutes: PropTypes.string,
    userCurrency: PropTypes.string,
    highlightId: PropTypes.string,
    // visible: PropTypes.bool,
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
