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
} from '@tanstack/react-table';
import { connect } from 'react-redux';
import { useNavigate, useLocation } from 'react-router';
import { DynamicLayoutContext } from '../../context';
import { getServiceURL } from '../../../../../utilities/rest';
import { buildColumnDefs, modifyRedirectUrl, DataTableColumnDef } from './tanstack/tableUtils';
import TanStackPagination from './tanstack/TanStackPagination';
import TanStackColumnPanel from './tanstack/TanStackColumnPanel';
import CellRendererDispatch from './tanstack/CellRendererDispatch';

interface DynamicTanStackGridProps {
    columnDefs: DataTableColumnDef[];
    id?: string;
    entries?: Record<string, unknown>[];
    sortModel?: Array<{ colId: string; sort: string; sortIndex?: number }>;
    filterModel?: Record<string, unknown>;
    rowSelection?: { mode?: string; enableClickSelection?: boolean };
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

function DynamicTanStackGrid(props: DynamicTanStackGridProps) {
    const {
        columnDefs,
        id,
        entries,
        sortModel,
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

    const rowData: Record<string, unknown>[] = useMemo(() => {
        if (entries) return entries;
        if (id && data) {
            return (Object as any).getByString(data, id) || (Object as any).getByString(variables, id) || [];
        }
        return [];
    }, [entries, id, data, variables]);

    const columns = useMemo(() => buildColumnDefs(columnDefs || []), [columnDefs]);

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
            columnOrder,
            columnSizing,
            columnPinning,
            ...(pagination ? { pagination: { pageIndex: 0, pageSize: (data as any)?.paginationPageSize || paginationPageSize } } : {}),
        },
        onSortingChange: setSorting,
        onColumnFiltersChange: setColumnFilters,
        onColumnVisibilityChange: setColumnVisibility,
        onColumnOrderChange: setColumnOrder,
        onColumnSizingChange: setColumnSizing,
        onColumnPinningChange: setColumnPinning,
        getCoreRowModel: getCoreRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        ...(pagination ? { getPaginationRowModel: getPaginationRowModel() } : {}),
        columnResizeMode: 'onChange',
        defaultColumn: {
            minSize: 50,
            size: 150,
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
            columnOrder: table.getState().columnOrder,
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
    }, [onColumnStatesChangedUrl, table]);

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

    // Column drag and drop
    const draggedColumn = useRef<string | null>(null);

    return (
        <div className="tanstack-grid-wrapper">
            {pagination && (
                <TanStackPagination table={table} pageSizeSelector={paginationPageSizeSelector} />
            )}
            <div className="d-flex gap-2">
                <TanStackColumnPanel
                    table={table}
                    resetGridStateUrl={resetGridStateUrl}
                    onReset={handleReset}
                />
            </div>
            <div className="table-responsive">
                <table className="table table-striped table-hover table-sm">
                    <thead>
                        {table.getHeaderGroups().map((headerGroup) => (
                            <tr key={headerGroup.id}>
                                {headerGroup.headers.map((header) => (
                                    <th
                                        key={header.id}
                                        style={{
                                            width: header.getSize(),
                                            position: header.column.getIsPinned() ? 'sticky' : undefined,
                                            left: header.column.getIsPinned() === 'left' ? 0 : undefined,
                                            right: header.column.getIsPinned() === 'right' ? 0 : undefined,
                                            zIndex: header.column.getIsPinned() ? 1 : undefined,
                                            cursor: header.column.getCanSort() ? 'pointer' : undefined,
                                        }}
                                        className={
                                            (header.column.columnDef.meta as any)?.headerClass?.join(' ') || ''
                                        }
                                        title={(header.column.columnDef.meta as any)?.headerTooltip}
                                        onClick={header.column.getToggleSortingHandler()}
                                        draggable
                                        onDragStart={() => { draggedColumn.current = header.column.id; }}
                                        onDragOver={(e) => e.preventDefault()}
                                        onDrop={() => {
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
                                        </div>
                                        {header.column.getCanResize() && (
                                            <div
                                                onMouseDown={header.getResizeHandler()}
                                                onTouchStart={header.getResizeHandler()}
                                                className="resizer"
                                                style={{
                                                    position: 'absolute',
                                                    right: 0,
                                                    top: 0,
                                                    height: '100%',
                                                    width: 4,
                                                    cursor: 'col-resize',
                                                    userSelect: 'none',
                                                    touchAction: 'none',
                                                }}
                                                onClick={(e) => e.stopPropagation()}
                                            />
                                        )}
                                    </th>
                                ))}
                            </tr>
                        ))}
                    </thead>
                    <tbody>
                        {table.getRowModel().rows.map((row) => {
                            const rowClasses: string[] = [];
                            if (getRowClassFn) {
                                const cls = getRowClassFn({ data: row.original, node: { data: row.original } });
                                if (cls) rowClasses.push(cls);
                            }
                            if (highlightRowId && (row.original as any).id === highlightRowId) {
                                rowClasses.push('table-warning');
                            }
                            return (
                                <tr
                                    key={row.id}
                                    data-row-id={(row.original as any).id}
                                    className={rowClasses.join(' ') || undefined}
                                    onClick={() => handleRowClick(row.original)}
                                    style={{ cursor: rowClickRedirectUrl ? 'pointer' : undefined }}
                                >
                                    {row.getVisibleCells().map((cell) => (
                                        <td
                                            key={cell.id}
                                            style={{
                                                width: cell.column.getSize(),
                                                position: cell.column.getIsPinned() ? 'sticky' : undefined,
                                                left: cell.column.getIsPinned() === 'left' ? 0 : undefined,
                                                right: cell.column.getIsPinned() === 'right' ? 0 : undefined,
                                                whiteSpace: (cell.column.columnDef.meta as any)?.wrapText ? 'pre-line' : undefined,
                                            }}
                                        >
                                            <CellRendererDispatch cell={cell} />
                                        </td>
                                    ))}
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
