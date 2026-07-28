import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    useReactTable,
    getCoreRowModel,
    getSortedRowModel,
    getFilteredRowModel,
    getPaginationRowModel,
    flexRender,
    SortingState,
    ColumnFiltersState,
    VisibilityState,
    ColumnOrderState,
    ColumnSizingState,
    ColumnPinningState,
    RowSelectionState,
    FilterFn,
} from '@tanstack/react-table';
import { connect } from 'react-redux';
import { useNavigate, useLocation } from 'react-router';
import { DynamicLayoutContext } from '../../context';
import { getServiceURL } from '../../../../../utilities/rest';
import { buildColumnDefs, modifyRedirectUrl, DataTableColumnDef } from './tanstack/tableUtils';
import TanStackPagination from './tanstack/TanStackPagination';
import TanStackColumnPanel from './tanstack/TanStackColumnPanel';
import TanStackColumnFilter from './tanstack/TanStackColumnFilter';
import CellRendererDispatch from './tanstack/CellRendererDispatch';

interface DynamicTanStackGridProps {
    columnDefs: DataTableColumnDef[];
    id?: string;
    entries?: Record<string, unknown>[];
    sortModel?: Array<{ colId: string; sort: string; sortIndex?: number }>;
    filterModel?: Record<string, unknown>;
    rowSelection?: { mode?: string; enableClickSelection?: boolean };
    selectedEntities?: number[];
    onSelectionChange?: (selectedRows: Record<string, unknown>[]) => void;
    rowClickRedirectUrl?: string;
    rowClickOpenModal?: boolean;
    rowClickFunction?: (row: Record<string, unknown>) => void;
    onColumnStatesChangedUrl?: string;
    resetGridStateUrl?: string;
    onGridApiReady?: (table: unknown) => void;
    pagination?: boolean;
    paginationPageSize?: number;
    paginationPageSizeSelector?: number[];
    getRowClass?: string;
    highlightId?: string;
    // From Redux:
    userLocale?: string;
    userDateFormat?: string;
    userTimestampFormatSeconds?: string;
    userTimestampFormatMinutes?: string;
    userCurrency?: string;
    userThousandSeparator?: string;
    userDecimalSeparator?: string;
}

// Custom filter: checks if cell value (resolved to string) is in the selected set
const setFilterFn: FilterFn<Record<string, unknown>> = (row, columnId, filterValue) => {
    if (!filterValue || !Array.isArray(filterValue) || filterValue.length === 0) return false;
    const cellValue = row.getValue(columnId);
    let cellStr: string;
    if (cellValue == null || cellValue === '') {
        cellStr = '';
    } else if (typeof cellValue === 'object') {
        if (Array.isArray(cellValue)) {
            return cellValue.some((item: any) =>
                filterValue.includes(item?.displayName ?? String(item)),
            );
        }
        cellStr = (cellValue as any).displayName ?? String(cellValue);
    } else {
        cellStr = String(cellValue);
    }
    return filterValue.includes(cellStr);
};

function DynamicTanStackGrid(props: DynamicTanStackGridProps) {
    const {
        columnDefs,
        id,
        entries,
        sortModel,
        rowSelection,
        selectedEntities,
        onSelectionChange,
        rowClickRedirectUrl,
        rowClickOpenModal,
        rowClickFunction,
        onColumnStatesChangedUrl,
        resetGridStateUrl,
        onGridApiReady,
        pagination,
        paginationPageSize = 50,
        paginationPageSizeSelector,
        getRowClass,
        highlightId,
    } = props;

    const { data, variables } = React.useContext(DynamicLayoutContext);
    const navigate = useNavigate();
    const location = useLocation();
    const [openFilterColumnId, setOpenFilterColumnId] = useState<string | null>(null);

    const rowData: Record<string, unknown>[] = useMemo(() => {
        if (entries) return entries;
        if (id && data) {
            return (Object as any).getByString(data, id) || (Object as any).getByString(variables, id) || [];
        }
        return [];
    }, [entries, id, data, variables]);

    const baseColumns = useMemo(() => buildColumnDefs(columnDefs || []), [columnDefs]);

    // Initialize sorting from sortModel
    const initialSorting: SortingState = useMemo(() => {
        if (!sortModel) return [];
        return sortModel
            .sort((a, b) => (a.sortIndex || 0) - (b.sortIndex || 0))
            .map((s) => ({ id: s.colId, desc: s.sort === 'desc' }));
    }, [sortModel]);

    // Initialize visibility from columnDefs
    const initialVisibility: VisibilityState = useMemo(() => {
        const vis: VisibilityState = {};
        (columnDefs || []).forEach((col) => {
            if (col.hide) vis[col.field] = false;
        });
        return vis;
    }, [columnDefs]);

    // Initialize column order from columnDefs
    const initialColumnOrder: ColumnOrderState = useMemo(
        () => (columnDefs || []).map((col) => col.field),
        [columnDefs],
    );

    // Initialize pinning from columnDefs
    const initialPinning: ColumnPinningState = useMemo(() => {
        const left: string[] = [];
        const right: string[] = [];
        (columnDefs || []).forEach((col) => {
            if (col.pinned === 'left') left.push(col.field);
            if (col.pinned === 'right') right.push(col.field);
        });
        return { left, right };
    }, [columnDefs]);

    const [sorting, setSorting] = useState<SortingState>(initialSorting);
    const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
    const [columnVisibility, setColumnVisibility] = useState<VisibilityState>(initialVisibility);
    const [columnOrder, setColumnOrder] = useState<ColumnOrderState>(initialColumnOrder);
    const [columnSizing, setColumnSizing] = useState<ColumnSizingState>({});
    const [columnPinning, setColumnPinning] = useState<ColumnPinningState>(initialPinning);

    const [paginationState, setPaginationState] = useState({
        pageIndex: 0,
        pageSize: (data as any)?.paginationPageSize || paginationPageSize || 50,
    });

    // Row selection state
    const enableSelection = rowSelection?.mode === 'multiRow';
    const initialRowSelection: RowSelectionState = useMemo(() => {
        if (!enableSelection || !selectedEntities || selectedEntities.length === 0) return {};
        const sel: RowSelectionState = {};
        rowData.forEach((row, idx) => {
            if (selectedEntities.includes((row as any).id)) {
                sel[idx] = true;
            }
        });
        return sel;
    }, [enableSelection, selectedEntities, rowData]);
    const [rowSelectionState, setRowSelectionState] = useState<RowSelectionState>(initialRowSelection);

    // Notify parent of selection changes
    useEffect(() => {
        if (!enableSelection || !onSelectionChange) return;
        const selected = Object.keys(rowSelectionState)
            .filter((key) => rowSelectionState[key])
            .map((key) => rowData[parseInt(key, 10)])
            .filter(Boolean);
        onSelectionChange(selected);
    }, [rowSelectionState, enableSelection, onSelectionChange, rowData]);

    // Sort columns by our own columnOrder state (TanStack's internal columnOrder has a memoization bug)
    const columns = useMemo(() => {
        if (!columnOrder || columnOrder.length === 0) return baseColumns;
        const orderMap = new Map(columnOrder.map((id, idx) => [id, idx]));
        return [...baseColumns].sort((a, b) => {
            const ai = orderMap.get(a.id as string) ?? 9999;
            const bi = orderMap.get(b.id as string) ?? 9999;
            return ai - bi;
        });
    }, [baseColumns, columnOrder]);

    // Row class function
    // eslint-disable-next-line no-new-func
    const getRowClassFn = useMemo(() => (getRowClass ? Function('params', getRowClass) as (params: unknown) => string : null), [getRowClass]);

    const table = useReactTable({
        data: rowData,
        columns,
        state: {
            sorting,
            columnFilters,
            columnVisibility,
            columnSizing,
            columnPinning,
            ...(pagination ? { pagination: paginationState } : {}),
            ...(enableSelection ? { rowSelection: rowSelectionState } : {}),
        },
        onSortingChange: setSorting,
        onColumnFiltersChange: setColumnFilters,
        onColumnVisibilityChange: setColumnVisibility,
        onColumnSizingChange: setColumnSizing,
        onColumnPinningChange: setColumnPinning,
        ...(pagination ? { onPaginationChange: setPaginationState } : {}),
        ...(enableSelection ? { onRowSelectionChange: setRowSelectionState, enableRowSelection: true } : {}),
        getCoreRowModel: getCoreRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        ...(pagination ? { getPaginationRowModel: getPaginationRowModel() } : {}),
        columnResizeMode: 'onChange',
        filterFns: { setFilter: setFilterFn },
        defaultColumn: {
            minSize: 50,
            size: 150,
            filterFn: setFilterFn,
        },
    });

    // Notify parent of table instance
    useEffect(() => {
        if (onGridApiReady) onGridApiReady(table);
    }, [table, onGridApiReady]);

    // Debounced column state persistence
    const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const postColumnState = useCallback(() => {
        if (!onColumnStatesChangedUrl) return;
        const state = {
            columnOrder,
            columnSizing: table.getState().columnSizing,
            columnVisibility: table.getState().columnVisibility,
            columnPinning: table.getState().columnPinning,
            sorting: table.getState().sorting,
            columnFilters: table.getState().columnFilters,
        };
        fetch(getServiceURL(onColumnStatesChangedUrl), {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(state),
        });
    }, [onColumnStatesChangedUrl, table, columnOrder]);

    const debouncedPostState = useCallback(() => {
        if (debounceTimer.current) clearTimeout(debounceTimer.current);
        debounceTimer.current = setTimeout(postColumnState, 500);
    }, [postColumnState]);

    // Post state on changes
    useEffect(() => {
        debouncedPostState();
    }, [sorting, columnVisibility, columnOrder, columnSizing, columnPinning, columnFilters]);

    // Row click handler
    const handleRowClick = useCallback((row: Record<string, unknown>) => {
        // Don't navigate if user is selecting text
        const selection = window.getSelection();
        if (selection && selection.toString().length > 0) return;

        if (rowClickFunction) {
            rowClickFunction(row);
            return;
        }
        if (!rowClickRedirectUrl) return;
        let url = modifyRedirectUrl(rowClickRedirectUrl, row);
        if (rowClickOpenModal) {
            url += url.includes('?') ? '&modal=true' : '?modal=true';
            navigate(url, { state: { background: location } });
        } else {
            navigate(url);
        }
    }, [rowClickRedirectUrl, rowClickOpenModal, rowClickFunction, navigate, location]);

    // Highlight row scrolling
    const highlightRowId = (data as any)?.highlightRowId || highlightId;
    const lastScrolledId = useRef<string | null>(null);

    useEffect(() => {
        if (!highlightRowId || lastScrolledId.current === highlightRowId) return;
        const timer = setTimeout(() => {
            const el = document.querySelector(`[data-row-id="${highlightRowId}"]`);
            if (el) {
                el.scrollIntoView({ behavior: 'smooth', block: 'start' });
                lastScrolledId.current = highlightRowId;
            }
        }, 200);
        return () => clearTimeout(timer);
    }, [highlightRowId, rowData]);

    // Handle reset
    const handleReset = useCallback((response: any) => {
        const vars = response?.variables;
        if (vars?.columnDefs) {
            const newCols = vars.columnDefs as DataTableColumnDef[];
            setColumnOrder(newCols.map((c) => c.field));
            setColumnVisibility(
                Object.fromEntries(newCols.filter((c) => c.hide).map((c) => [c.field, false])),
            );
            setColumnSizing({});
            if (vars.sortModel) {
                setSorting(
                    vars.sortModel
                        .sort((a: any, b: any) => (a.sortIndex || 0) - (b.sortIndex || 0))
                        .map((s: any) => ({ id: s.colId, desc: s.sort === 'desc' })),
                );
            }
            setColumnFilters([]);
        }
    }, []);

    // Selection: track anchor row for Shift-click range selection
    const anchorRowIdx = useRef<number | null>(null);

    const handleRowSelection = useCallback((rowIdx: number, e: React.MouseEvent) => {
        if (!enableSelection) return;
        setRowSelectionState((prev) => {
            if (e.shiftKey && anchorRowIdx.current !== null) {
                // Range select from anchor to current
                const start = Math.min(anchorRowIdx.current, rowIdx);
                const end = Math.max(anchorRowIdx.current, rowIdx);
                const next: RowSelectionState = {};
                for (let i = start; i <= end; i++) {
                    next[i] = true;
                }
                return next;
            }
            if (e.ctrlKey || e.metaKey) {
                // Toggle single row additively, update anchor
                anchorRowIdx.current = rowIdx;
                const next = { ...prev };
                if (next[rowIdx]) {
                    delete next[rowIdx];
                } else {
                    next[rowIdx] = true;
                }
                return next;
            }
            // Plain click: select only this row
            anchorRowIdx.current = rowIdx;
            return { [rowIdx]: true };
        });
        setFocusedRowIdx(rowIdx);
    }, [enableSelection]);

    // Keyboard navigation for selection
    const [focusedRowIdx, setFocusedRowIdx] = useState<number | null>(null);
    const tbodyRef = useRef<HTMLTableSectionElement>(null);

    const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
        if (!enableSelection) return;
        const rows = table.getRowModel().rows;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            const next = Math.min((focusedRowIdx ?? -1) + 1, rows.length - 1);
            setFocusedRowIdx(next);
            if (e.shiftKey) {
                setRowSelectionState((prev) => ({ ...prev, [next]: true }));
            }
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            const next = Math.max((focusedRowIdx ?? 1) - 1, 0);
            setFocusedRowIdx(next);
            if (e.shiftKey) {
                setRowSelectionState((prev) => ({ ...prev, [next]: true }));
            }
        } else if (e.key === ' ' && focusedRowIdx !== null) {
            e.preventDefault();
            setRowSelectionState((prev) => {
                const next = { ...prev };
                if (next[focusedRowIdx]) {
                    delete next[focusedRowIdx];
                } else {
                    next[focusedRowIdx] = true;
                }
                return next;
            });
            anchorRowIdx.current = focusedRowIdx;
        }
    }, [enableSelection, table, focusedRowIdx]);

    // Column drag and drop — disabled while resizing
    const draggedColumn = useRef<string | null>(null);
    const isResizing = useRef(false);

    return (
        <div className="tanstack-grid-wrapper">
            {pagination && (
                <TanStackPagination table={table} pageSizeSelector={paginationPageSizeSelector} />
            )}
            <div className="d-flex gap-2">
                <TanStackColumnPanel
                    table={table}
                    columnOrder={columnOrder}
                    onColumnOrderChange={setColumnOrder}
                    resetGridStateUrl={resetGridStateUrl}
                    onReset={handleReset}
                />
            </div>
            <div className="table-responsive">
                <table className="table table-striped table-hover table-sm" style={{ tableLayout: 'fixed', width: Math.max(table.getTotalSize(), 100) }}>
                    <thead>
                        {table.getHeaderGroups().map((headerGroup) => (
                            <tr key={headerGroup.id}>
                                {enableSelection && (
                                    <th style={{ width: 40, textAlign: 'center', position: 'sticky', left: 0, zIndex: 2, backgroundColor: 'var(--bs-table-bg, #fff)' }}>
                                        <input
                                            type="checkbox"
                                            checked={table.getIsAllRowsSelected()}
                                            onChange={table.getToggleAllRowsSelectedHandler()}
                                        />
                                    </th>
                                )}
                                {headerGroup.headers.map((header) => {
                                    const headerMeta = header.column.columnDef.meta as any;
                                    const isHeaderNumeric = headerMeta?.type === 'numericColumn' || headerMeta?.type === 'rightAligned';
                                    return (
                                    <th
                                        key={header.id}
                                        style={{
                                            width: header.getSize(),
                                            textAlign: isHeaderNumeric ? 'right' : undefined,
                                            position: header.column.getIsPinned() ? 'sticky' : undefined,
                                            left: header.column.getIsPinned() === 'left'
                                                ? header.column.getStart('left')
                                                : undefined,
                                            right: header.column.getIsPinned() === 'right' ? 0 : undefined,
                                            zIndex: header.column.getIsPinned() ? 2 : undefined,
                                            backgroundColor: header.column.getIsPinned() ? 'var(--bs-table-bg, #fff)' : undefined,
                                            cursor: header.column.getCanSort() ? 'pointer' : undefined,
                                        }}
                                        className={
                                            headerMeta?.headerClass?.join(' ') || ''
                                        }
                                        title={headerMeta?.headerTooltip}
                                        onClick={header.column.getToggleSortingHandler()}
                                        draggable={!isResizing.current && !header.column.getIsPinned()}
                                        onDragStart={(e) => {
                                            if (isResizing.current || header.column.getIsPinned()) {
                                                e.preventDefault();
                                                return;
                                            }
                                            draggedColumn.current = header.column.id;
                                        }}
                                        onDragOver={(e) => e.preventDefault()}
                                        onDrop={() => {
                                            if (header.column.getIsPinned()) {
                                                draggedColumn.current = null;
                                                return;
                                            }
                                            if (draggedColumn.current && draggedColumn.current !== header.column.id) {
                                                const newOrder = [...columnOrder];
                                                const fromIdx = newOrder.indexOf(draggedColumn.current);
                                                const toIdx = newOrder.indexOf(header.column.id);
                                                if (fromIdx !== -1 && toIdx !== -1) {
                                                    newOrder.splice(fromIdx, 1);
                                                    newOrder.splice(toIdx, 0, draggedColumn.current);
                                                    setColumnOrder(newOrder);
                                                }
                                            }
                                            draggedColumn.current = null;
                                        }}
                                    >
                                        <div className="d-flex align-items-center gap-1">
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(header.column.columnDef.header, header.getContext())}
                                            {header.column.getIsSorted() === 'asc' && ' ▲'}
                                            {header.column.getIsSorted() === 'desc' && ' ▼'}
                                            {header.column.getCanFilter() && (
                                                <span
                                                    className={`ms-auto ${header.column.getIsFiltered() ? 'text-primary' : 'text-muted'}`}
                                                    style={{ cursor: 'pointer', fontSize: '0.7rem' }}
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        setOpenFilterColumnId(
                                                            openFilterColumnId === header.column.id ? null : header.column.id,
                                                        );
                                                    }}
                                                    title="Filter"
                                                >
                                                    <i className="fas fa-filter" />
                                                </span>
                                            )}
                                        </div>
                                        {openFilterColumnId === header.column.id && (
                                            <TanStackColumnFilter
                                                column={header.column}
                                                table={table}
                                                onClose={() => setOpenFilterColumnId(null)}
                                            />
                                        )}
                                        {header.column.getCanResize() && (
                                            <div
                                                onMouseDown={(e) => {
                                                    isResizing.current = true;
                                                    header.getResizeHandler()(e);
                                                    const onMouseUp = () => {
                                                        isResizing.current = false;
                                                        document.removeEventListener('mouseup', onMouseUp);
                                                    };
                                                    document.addEventListener('mouseup', onMouseUp);
                                                }}
                                                onTouchStart={(e) => {
                                                    isResizing.current = true;
                                                    header.getResizeHandler()(e);
                                                    const onTouchEnd = () => {
                                                        isResizing.current = false;
                                                        document.removeEventListener('touchend', onTouchEnd);
                                                    };
                                                    document.addEventListener('touchend', onTouchEnd);
                                                }}
                                                onDragStart={(e) => {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                }}
                                                draggable={false}
                                                className="resizer"
                                                style={{
                                                    position: 'absolute',
                                                    right: 0,
                                                    top: 0,
                                                    height: '100%',
                                                    width: 6,
                                                    cursor: 'col-resize',
                                                    userSelect: 'none',
                                                    touchAction: 'none',
                                                }}
                                                onClick={(e) => e.stopPropagation()}
                                            />
                                        )}
                                    </th>
                                    );
                                })}
                            </tr>
                        ))}
                    </thead>
                    <tbody ref={tbodyRef} tabIndex={enableSelection ? 0 : undefined} onKeyDown={enableSelection ? handleKeyDown : undefined}>
                        {table.getRowModel().rows.map((row, rowIdx) => {
                            const rowClasses: string[] = [];
                            if (getRowClassFn) {
                                const cls = getRowClassFn({ data: row.original, node: { data: row.original } });
                                if (cls) rowClasses.push(cls);
                            }
                            if (enableSelection && row.getIsSelected()) {
                                rowClasses.push('table-primary');
                            }
                            if (enableSelection && focusedRowIdx === rowIdx) {
                                rowClasses.push('table-active');
                            }
                            if (highlightRowId && (row.original as any).id === highlightRowId) {
                                rowClasses.push('table-warning');
                            }
                            return (
                                <tr
                                    key={row.id}
                                    data-row-id={(row.original as any).id}
                                    className={rowClasses.join(' ') || undefined}
                                    onClick={(e) => {
                                        if (enableSelection) {
                                            if (e.shiftKey) {
                                                e.preventDefault();
                                                window.getSelection()?.removeAllRanges();
                                            }
                                            handleRowSelection(rowIdx, e);
                                        } else {
                                            handleRowClick(row.original);
                                        }
                                    }}
                                    style={{
                                        cursor: (rowClickRedirectUrl || enableSelection) ? 'pointer' : undefined,
                                        userSelect: enableSelection ? 'none' : undefined,
                                    }}
                                >
                                    {enableSelection && (
                                        <td style={{ width: 40, textAlign: 'center', position: 'sticky', left: 0, zIndex: 1, backgroundColor: 'var(--bs-table-bg, #fff)' }} onClick={(e) => e.stopPropagation()}>
                                            <input
                                                type="checkbox"
                                                checked={row.getIsSelected()}
                                                onChange={row.getToggleSelectedHandler()}
                                            />
                                        </td>
                                    )}
                                    {row.getVisibleCells().map((cell) => {
                                        const meta = cell.column.columnDef.meta as any;
                                        const tooltipField = meta?.tooltipField;
                                        const tooltip = tooltipField ? String((row.original as any)[tooltipField] ?? '') : undefined;
                                        const isNumeric = meta?.type === 'numericColumn' || meta?.type === 'rightAligned';
                                        return (
                                        <td
                                            key={cell.id}
                                            title={tooltip || undefined}
                                            style={{
                                                width: cell.column.getSize(),
                                                textAlign: isNumeric ? 'right' : undefined,
                                                position: cell.column.getIsPinned() ? 'sticky' : undefined,
                                                left: cell.column.getIsPinned() === 'left'
                                                    ? cell.column.getStart('left')
                                                    : undefined,
                                                right: cell.column.getIsPinned() === 'right' ? 0 : undefined,
                                                zIndex: cell.column.getIsPinned() ? 1 : undefined,
                                                backgroundColor: cell.column.getIsPinned() ? 'var(--bs-table-bg, #fff)' : undefined,
                                                whiteSpace: meta?.wrapText ? 'pre-line' : undefined,
                                            }}
                                        >
                                            <CellRendererDispatch cell={cell} />
                                        </td>
                                        );
                                    })}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
            {pagination && table.getPageCount() > 1 && (
                <TanStackPagination table={table} pageSizeSelector={paginationPageSizeSelector} />
            )}
        </div>
    );
}

const mapStateToProps = ({ authentication }: any) => ({
    userLocale: authentication?.user?.locale,
    userDateFormat: authentication?.user?.dateFormat,
    userThousandSeparator: authentication?.user?.thousandSeparator,
    userDecimalSeparator: authentication?.user?.decimalSeparator,
    userTimestampFormatSeconds: authentication?.user?.timestampFormatSeconds,
    userTimestampFormatMinutes: authentication?.user?.timestampFormatMinutes,
    userCurrency: authentication?.user?.currency,
});

export default connect(mapStateToProps)(DynamicTanStackGrid);
