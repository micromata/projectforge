import AwesomeDebouncePromise from 'awesome-debounce-promise';
import PropTypes from 'prop-types';
import React, { useMemo, useRef, useEffect, useState, lazy, Suspense } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { LicenseManager, ModuleRegistry, AllEnterpriseModule, themeBalham } from 'ag-grid-enterprise';
import { connect } from 'react-redux';
import { useNavigate, useLocation } from 'react-router';
import { DynamicLayoutContext } from '../../context';
import Formatter from '../../../Formatter';
import { getServiceURL } from '../../../../../utilities/rest';
import { AG_GRID_LOCALE_DE } from './agGridLocalization_de';
import formatterFormat from '../../../FormatterFormat';
import DynamicAgGridDiffCell from './DynamicAgGridDiffCell';
import ImportStatusCell from './ImportStatusCell';
import MultilineCell from './MultilineCell';
import OpenModalLinkCell from '../customized/components/OpenModalLinkCell';

// Lazy load the customized cell renderer to avoid circular dependencies
const DynamicAgGridCustomizedCellLazy = lazy(() => import('./DynamicAgGridCustomizedCell'));

// Wrapper component that handles Suspense for the lazy-loaded customized cell renderer
function CustomizedCellWrapper(props) {
    return (
        <Suspense fallback={<span>...</span>}>
            <DynamicAgGridCustomizedCellLazy {...props} />
        </Suspense>
    );
}

LicenseManager.setLicenseKey('Using_this_{AG_Grid}_Enterprise_key_{AG-089620}_in_excess_of_the_licence_granted_is_not_permitted___Please_report_misuse_to_legal@ag-grid.com___For_help_with_changing_this_key_please_contact_info@ag-grid.com___{Micromata_GmbH}_is_granted_a_{Single_Application}_Developer_License_for_the_application_{ProjectForge}_only_for_{1}_Front-End_JavaScript_developer___All_Front-End_JavaScript_developers_working_on_{ProjectForge}_need_to_be_licensed___{ProjectForge}_has_not_been_granted_a_Deployment_License_Add-on___This_key_works_with_{AG_Grid}_Enterprise_versions_released_before_{14_July_2026}____[v3]_[01]_MTc4Mzk4MzYwMDAwMA==932cdb76359c2b516156ac62da0bd954');
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
        filterModel,
        rowSelection,
        rowClickRedirectUrl,
        rowClickFunction,
        rowClickOpenModal,
        onCellClicked,
        onColumnStatesChangedUrl,
        resetGridStateUrl,
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
    const navigate = useNavigate();
    const location = useLocation();
    const [gridApi, setGridApi] = useState();
    const gridRef = useRef();
    // const gridStyle = React.useMemo(() => ({ width: '100%' }), []);
    const [processedColumnDefs, setProcessedColumnDefs] = useState([]);
    const rowData = entries || Object.getByString(data, id) || Object.getByString(variables, id) || '';
    const { selectedEntityIds } = data;
    const lastScrolledId = useRef(null);
    const pendingScrollId = useRef(null);

    const scrollToHighlightedRow = React.useCallback((clearPending = false) => {
        const highlightRowId = pendingScrollId.current;
        if (!highlightRowId || !gridApi) {
            return;
        }

        let highlightIndex;
        gridApi.forEachNode((rowNode, index) => {
            if (highlightIndex === undefined && rowNode.data?.id === highlightRowId) {
                highlightIndex = index;
            }
        });

        if (highlightIndex !== undefined) {
            try {
                // Since domLayout="autoHeight", the grid doesn't have
                // internal scrolling
                // We need to scroll the browser window to the row's DOM element
                const rowElement = document.querySelector(
                    `.ag-row[row-index="${highlightIndex}"]`,
                );
                if (rowElement) {
                    rowElement.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start',
                    });
                    lastScrolledId.current = highlightRowId;
                    if (clearPending) {
                        pendingScrollId.current = null;
                    }
                }
            } catch (error) {
                // Silently ignore scroll errors
            }
        }
    }, [gridApi]);

    const showHighlightedRow = React.useCallback(() => {
        const highlightRowId = data.highlightRowId || highlightId;

        // Only scroll if we haven't already scrolled to this specific ID
        if (highlightRowId && lastScrolledId.current !== highlightRowId) {
            pendingScrollId.current = highlightRowId;
        }
    }, [data.highlightRowId, highlightId]);

    const onGridReady = React.useCallback((params) => {
        setGridApi(params.api);
        params.api.applyColumnState({
            state: sortModel,
            defaultState: { sort: null },
        });

        // Restore filter model from server if available
        if (filterModel) {
            params.api.setFilterModel(filterModel);
        }

        if (onGridApiReady) {
            onGridApiReady(params.api, params.columnApi);
        }
        if (!height) {
            // params.api.setDomLayout('autoHeight'); // Needed to get maximum height.
        }
        showHighlightedRow();
    }, [selectedEntityIds, setGridApi, filterModel, showHighlightedRow]);

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

    React.useEffect(() => {
        showHighlightedRow();
    }, [showHighlightedRow]);

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

    const modifyRedirectUrl = (redirectUrl, row) => {
        let url = redirectUrl;

        // Replace field placeholders in the URL with actual row data values
        // Supports: {fieldName}, :fieldName, /fieldName in path, fieldName in query params
        if (row) {
            // Always process 'id' field first, even if not present in row
            const idValue = (row.id == null) ? 'undefined' : String(row.id);
            url = url.replace('{id}', idValue);
            url = url.replace(':id', idValue);
            url = url.replace(/\/id(?=[?/]|$)/g, `/${idValue}`);

            // Process all other fields in the row
            Object.keys(row).forEach((fieldName) => {
                if (fieldName === 'id') return; // Already processed above

                // Use 'undefined' string for null/undefined values, otherwise convert to string
                const fieldValue = (row[fieldName] == null) ? 'undefined' : String(row[fieldName]);

                // Replace {fieldName} placeholder (e.g., /edit/{campaignId})
                url = url.replace(`{${fieldName}}`, fieldValue);

                // Replace :fieldName placeholder (React Router style)
                url = url.replace(`:${fieldName}`, fieldValue);

                // Replace /fieldName in path segments
                const pathSegmentPattern = new RegExp(`/${fieldName}(?=[?/]|$)`, 'g');
                url = url.replace(pathSegmentPattern, `/${fieldValue}`);

                // Replace fieldName=fieldName in query parameters
                const queryParamPattern = new RegExp(`${fieldName}=${fieldName}(?=&|$)`, 'g');
                url = url.replace(queryParamPattern, `${fieldName}=${fieldValue}`);
            });
        }

        return url;
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
        let redirectUrl = modifyRedirectUrl(rowClickRedirectUrl, event.data);
        if (rowClickOpenModal) {
            // Add modal=true query parameter so backend knows it was opened in modal context
            redirectUrl += redirectUrl.includes('?') ? '&modal=true' : '?modal=true';

            navigate(redirectUrl, { state: { background: location } });
        } else {
            navigate(redirectUrl);
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
        const firstSelectedRow = selectedRows[0];
        if (!firstSelectedRow || !firstSelectedRow.id) {
            // Can't detect id.
            return;
        }
        navigate(modifyRedirectUrl(rowClickRedirectUrl, firstSelectedRow));
    };

    const postColumnStates = (event) => {
        if (onColumnStatesChangedUrl) {
            const columnState = event.api.getColumnState();
            const currentFilterModel = event.api.getFilterModel();
            fetch(getServiceURL(onColumnStatesChangedUrl), {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    columnState,
                    filterModel: currentFilterModel,
                }),
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

    const onFilterChanged = async (event) => {
        await postColumnStatesDebounced(event);
    };

    const onModelUpdated = React.useCallback(() => {
        // Grid model has been updated (fires reliably when returning from edit pages)
        // For autoHeight with wraptext and async image loading, use multiple attempts
        // to ensure scrolling happens after all heights are calculated
        if (pendingScrollId.current) {
            // First attempt: quick scroll for simple cases
            setTimeout(() => {
                if (pendingScrollId.current) {
                    scrollToHighlightedRow(false);
                }
            }, 100);
            // Second attempt: medium delay for wraptext calculations
            setTimeout(() => {
                if (pendingScrollId.current) {
                    scrollToHighlightedRow(false);
                }
            }, 300);
            // Final attempt: long delay for async image loading, then clear pending
            setTimeout(() => {
                if (pendingScrollId.current) {
                    scrollToHighlightedRow(true);
                }
            }, 500);
        }
    }, [scrollToHighlightedRow]);

    const onGridSizeChanged = React.useCallback(() => {
        // Grid size has changed after height calculations (backup for autoHeight)
        // This fires when row heights change significantly (e.g., images loaded)
        // Clear pending since grid size change indicates rendering is complete
        if (pendingScrollId.current) {
            scrollToHighlightedRow(true);
        }
    }, [scrollToHighlightedRow]);

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
        customized: CustomizedCellWrapper,
        importStatusCell: ImportStatusCell,
        multilineCell: MultilineCell,
        OpenModalLinkCell,
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

    const getMainMenuItems = React.useCallback((params) => {
        const menuItems = params.defaultItems.slice();

        // Find and replace the resetColumns menu item
        const resetIndex = menuItems.findIndex((item) => item === 'resetColumns');
        if (resetIndex !== -1 && resetGridStateUrl && gridApi) {
            menuItems[resetIndex] = {
                name: getLocaleText({ key: 'resetColumns', defaultValue: 'Reset Columns' }),
                action: () => {
                    // Call backend to clear grid state and get fresh column definitions
                    fetch(getServiceURL(resetGridStateUrl), {
                        method: 'GET',
                        credentials: 'include',
                    })
                        .then((response) => response.json())
                        .then((responseAction) => {
                            // Server has already extracted columnDefs for us!
                            const {
                                columnDefs: resetColumnDefs,
                                sortModel: resetSortModel,
                                filterModel: resetFilterModel,
                            } = responseAction.variables || {};

                            if (resetColumnDefs) {
                                // Update AG Grid directly via API
                                gridApi.setGridOption('columnDefs', resetColumnDefs);

                                // Reset all column states (width, position, etc.) to defaults
                                gridApi.resetColumnState();

                                if (resetSortModel) {
                                    gridApi.applyColumnState({
                                        state: resetSortModel,
                                        defaultState: { sort: null },
                                    });
                                }

                                // Reset filters
                                if (resetFilterModel !== undefined) {
                                    gridApi.setFilterModel(resetFilterModel);
                                }
                            }
                        })
                        .catch((error) => {
                            // eslint-disable-next-line no-console
                            console.error('Error resetting grid state:', error);
                        });
                },
            };
        }

        return menuItems;
    }, [resetGridStateUrl, getLocaleText, gridApi]);

    return React.useMemo(
        () => (
            <div
                style={{ minWidth: '100%', height }}
                className="ag-grid-wrapper-pagination-top"
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
                    onModelUpdated={onModelUpdated}
                    onSelectionChanged={onSelectionChanged}
                    onSortChanged={onSortChanged}
                    onColumnMoved={onColumnMoved}
                    onColumnResized={onColumnResized}
                    onColumnVisible={onColumnVisible}
                    onColumnPinned={onColumnPinned}
                    onFilterChanged={onFilterChanged}
                    onRowClicked={onRowClicked}
                    onCellClicked={onCellClicked}
                    onGridSizeChanged={onGridSizeChanged}
                    pagination={pagination}
                    paginationPageSize={data.paginationPageSize || paginationPageSize}
                    paginationPageSizeSelector={paginationPageSizeSelector}
                    rowClass={rowClass}
                    getRowClass={usedGetRowClass}
                    suppressHorizontalScroll={false}
                    accentedSort
                    cellSelection
                    getLocaleText={getLocaleText}
                    getMainMenuItems={getMainMenuItems}
                    processCellForClipboard={processCellForClipboard}
                    // processCellCallback={processCellCallback}
                    tooltipShowDelay={0}
                    suppressScrollOnNewData
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
            entries,
            selectionColumnDef,
            sortModel,
            rowSelection,
            paginationPageSize,
            getMainMenuItems,
        ],
    );
}

DynamicAgGrid.propTypes = {
    columnDefs: PropTypes.arrayOf(PropTypes.shape({
        field: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    selectionColumnDef: PropTypes.shape({
        pinned: PropTypes.string,
        resizable: PropTypes.bool,
        sortable: PropTypes.bool,
    }),
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
    resetGridStateUrl: PropTypes.string,
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
