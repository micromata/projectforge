import React from 'react';
import DynamicTanStackGrid from './DynamicTanStackGrid';
import { DataTableColumnDef } from './tanstack/tableUtils';

interface DynamicListPageTanStackGridProps {
    columnDefs: DataTableColumnDef[];
    id?: string;
    sortModel?: Array<{ colId: string; sort: string; sortIndex?: number }>;
    filterModel?: Record<string, unknown>;
    rowClickRedirectUrl?: string;
    rowClickOpenModal?: boolean;
    onColumnStatesChangedUrl?: string;
    resetGridStateUrl?: string;
    pagination?: boolean;
    paginationPageSize?: number;
    paginationPageSizeSelector?: number[];
    getRowClass?: string;
}

/**
 * The grid of a list page of the legacy React app. Without a multi selection: the mass updates are
 * pages of projectforge-next.
 */
function DynamicListPageTanStackGrid({
    columnDefs,
    id,
    sortModel,
    filterModel,
    rowClickRedirectUrl,
    rowClickOpenModal,
    onColumnStatesChangedUrl,
    resetGridStateUrl,
    pagination,
    paginationPageSize,
    paginationPageSizeSelector,
    getRowClass,
}: DynamicListPageTanStackGridProps) {
    return (
        <DynamicTanStackGrid
            columnDefs={columnDefs}
            id={id}
            sortModel={sortModel}
            filterModel={filterModel}
            rowClickRedirectUrl={rowClickRedirectUrl}
            rowClickOpenModal={rowClickOpenModal}
            onColumnStatesChangedUrl={onColumnStatesChangedUrl}
            resetGridStateUrl={resetGridStateUrl}
            pagination={pagination}
            paginationPageSize={paginationPageSize}
            paginationPageSizeSelector={paginationPageSizeSelector}
            getRowClass={getRowClass}
        />
    );
}

export default DynamicListPageTanStackGrid;
