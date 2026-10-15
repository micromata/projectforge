import React from 'react';
import { Table } from '@tanstack/react-table';

interface TanStackPaginationProps {
    table: Table<Record<string, unknown>>;
    pageSizeSelector?: number[];
}

export default function TanStackPagination({
    table,
    pageSizeSelector = [25, 50, 100, 200, 500, 1000],
}: TanStackPaginationProps) {
    const pageIndex = table.getState().pagination.pageIndex;
    const pageSize = table.getState().pagination.pageSize;
    const totalRows = table.getFilteredRowModel().rows.length;
    const pageCount = table.getPageCount();
    const from = pageIndex * pageSize + 1;
    const to = Math.min((pageIndex + 1) * pageSize, totalRows);

    return (
        <div className="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
            <div className="d-flex align-items-center gap-2">
                <select
                    className="form-select form-select-sm"
                    style={{ width: 'auto' }}
                    value={pageSize}
                    onChange={(e) => table.setPageSize(Number(e.target.value))}
                >
                    {pageSizeSelector.map((size) => (
                        <option key={size} value={size}>{size}</option>
                    ))}
                </select>
                <span className="text-muted small">
                    {totalRows > 0 ? `${from}–${to} von ${totalRows}` : 'Keine Einträge'}
                </span>
            </div>
            <div className="btn-group btn-group-sm">
                <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => table.setPageIndex(0)}
                    disabled={!table.getCanPreviousPage()}
                >
                    «
                </button>
                <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => table.previousPage()}
                    disabled={!table.getCanPreviousPage()}
                >
                    ‹
                </button>
                <span className="btn btn-outline-secondary disabled">
                    {pageCount > 0 ? `${pageIndex + 1} / ${pageCount}` : '–'}
                </span>
                <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => table.nextPage()}
                    disabled={!table.getCanNextPage()}
                >
                    ›
                </button>
                <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => table.setPageIndex(pageCount - 1)}
                    disabled={!table.getCanNextPage()}
                >
                    »
                </button>
            </div>
        </div>
    );
}
