import React, { useCallback, useContext, useState } from 'react';
import DynamicTanStackGrid from './DynamicTanStackGrid';
import { DynamicLayoutContext } from '../../context';
import { fetchJsonPost, getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import history from '../../../../../utilities/history';
import DynamicButton from '../DynamicButton';
import { DataTableColumnDef } from './tanstack/tableUtils';

interface DynamicListPageTanStackGridProps {
    columnDefs: DataTableColumnDef[];
    selectionColumnDef?: Record<string, unknown>;
    id?: string;
    sortModel?: Array<{ colId: string; sort: string; sortIndex?: number }>;
    filterModel?: Record<string, unknown>;
    rowSelection?: { mode?: string; enableClickSelection?: boolean; enableSelectionWithoutKeys?: boolean };
    rowClickRedirectUrl?: string;
    rowClickOpenModal?: boolean;
    onColumnStatesChangedUrl?: string;
    resetGridStateUrl?: string;
    multiSelectButtonTitle?: string;
    multiSelectButtonConfirmMessage?: string;
    urlAfterMultiSelect?: string;
    handleCancelUrl?: string;
    pagination?: boolean;
    paginationPageSize?: number;
    paginationPageSizeSelector?: number[];
    getRowClass?: string;
    rowMultiSelectWithClick?: boolean;
}

function DynamicListPageTanStackGrid({
    columnDefs,
    id,
    sortModel,
    filterModel,
    rowSelection,
    rowClickRedirectUrl,
    rowClickOpenModal,
    onColumnStatesChangedUrl,
    resetGridStateUrl,
    multiSelectButtonTitle,
    multiSelectButtonConfirmMessage,
    urlAfterMultiSelect,
    handleCancelUrl,
    pagination,
    paginationPageSize,
    paginationPageSizeSelector,
    getRowClass,
}: DynamicListPageTanStackGridProps) {
    const { ui, callAction } = useContext(DynamicLayoutContext);
    const [selectedRows, setSelectedRows] = useState<Record<string, unknown>[]>([]);

    const handleCancel = useCallback(() => {
        if (!handleCancelUrl) return;
        fetch(getServiceURL(handleCancelUrl), {
            method: 'GET',
            credentials: 'include',
        })
            .then(handleHTTPErrors)
            .then((response) => response.text())
            .then((url) => {
                history.push(url);
            });
    }, [handleCancelUrl]);

    const handleClick = useCallback(() => {
        if (!urlAfterMultiSelect) return;
        const selectedIds = selectedRows.map((row) => (row as any).id);
        fetchJsonPost(
            urlAfterMultiSelect,
            { selectedIds },
            (json: any) => {
                callAction({ responseAction: json });
            },
        );
    }, [selectedRows, urlAfterMultiSelect, callAction]);

    return (
        <div>
            {multiSelectButtonTitle && (
                <>
                    <DynamicButton
                        id="cancel"
                        title={(ui as any).translations?.cancel || 'cancel'}
                        handleButtonClick={handleCancel}
                        color="danger"
                        outline
                    />
                    <DynamicButton
                        id="next"
                        title={multiSelectButtonTitle || 'next'}
                        handleButtonClick={handleClick}
                        color="success"
                        outline
                        confirmMessage={multiSelectButtonConfirmMessage}
                    />
                </>
            )}
            <DynamicTanStackGrid
                columnDefs={columnDefs}
                id={id}
                sortModel={sortModel}
                filterModel={filterModel}
                rowSelection={rowSelection}
                rowClickRedirectUrl={rowClickRedirectUrl}
                rowClickOpenModal={rowClickOpenModal}
                onColumnStatesChangedUrl={onColumnStatesChangedUrl}
                resetGridStateUrl={resetGridStateUrl}
                pagination={pagination}
                paginationPageSize={paginationPageSize}
                paginationPageSizeSelector={paginationPageSizeSelector}
                getRowClass={getRowClass}
            />
        </div>
    );
}

export default DynamicListPageTanStackGrid;
