import React, {
    useCallback, useContext, useMemo, useRef, useState,
} from 'react';
import {
    Badge, Modal, ModalBody, ModalHeader,
} from 'reactstrap';
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
    rowSelection?: { mode?: string; enableClickSelection?: boolean };
    selectedEntities?: number[];
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

type RowData = Record<string, unknown>;

// Resolve a human-readable label for a selected row from its column values (first visible columns).
// Cell values may be plain scalars or objects carrying a `displayName`.
const resolveCellText = (value: unknown): string => {
    if (value == null || value === '') return '';
    if (typeof value === 'object') {
        if (Array.isArray(value)) {
            return value.map((item: any) => item?.displayName ?? String(item)).join(', ');
        }
        return (value as any).displayName ?? '';
    }
    return String(value);
};

function DynamicListPageTanStackGrid({
    columnDefs,
    id,
    sortModel,
    filterModel,
    rowSelection,
    selectedEntities,
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
    const translations = (ui as any)?.translations || {};

    // The complete, authoritative selection kept across server-side filter changes: id -> row data.
    // Reconciled from the grid's selection callback so that rows selected under a previous filter
    // run are not lost when they leave the currently loaded rowData.
    const [selectedRowsMap, setSelectedRowsMap] = useState<Map<string, RowData>>(new Map());
    const [showSelectedModal, setShowSelectedModal] = useState(false);
    // The TanStack table instance, captured so we can reset / mutate the grid selection from here.
    const tableRef = useRef<any>(null);

    const handleSelectionChange = useCallback(
        ({ ids, rowsById }: { ids: string[]; rowsById: Record<string, RowData> }) => {
            setSelectedRowsMap((prev) => {
                const next = new Map<string, RowData>();
                ids.forEach((entityId) => {
                    const row = rowsById[entityId] ?? prev.get(entityId) ?? { id: entityId };
                    next.set(entityId, row);
                });
                return next;
            });
        },
        [],
    );

    const deselectOne = useCallback((entityId: string) => {
        setSelectedRowsMap((prev) => {
            const next = new Map(prev);
            next.delete(entityId);
            return next;
        });
        // Keep the grid's selection state in sync (checkbox / row highlight).
        tableRef.current?.setRowSelection?.((prev: Record<string, boolean>) => {
            const next = { ...prev };
            delete next[entityId];
            return next;
        });
    }, []);

    const deselectAll = useCallback(() => {
        setSelectedRowsMap(new Map());
        tableRef.current?.resetRowSelection?.();
    }, []);

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
        // Post the complete selection (across all filter runs), not just the currently loaded rows.
        // Selection keys are strings (see getRowId); send numeric ids as numbers to match the
        // payload the backend expects.
        const selectedIds = Array.from(selectedRowsMap.keys()).map((key) => {
            const num = Number(key);
            return Number.isNaN(num) ? key : num;
        });
        fetchJsonPost(
            urlAfterMultiSelect,
            { selectedIds },
            (json: any) => {
                callAction({ responseAction: json });
            },
        );
    }, [selectedRowsMap, urlAfterMultiSelect, callAction]);

    // Columns used to label a selected row in the "show selected" list (first two visible columns).
    const labelColumns = useMemo(
        () => (columnDefs || []).filter((c) => !c.hide).slice(0, 2),
        [columnDefs],
    );

    const selectedLabel = useCallback((row: Record<string, unknown>): string => {
        const parts = labelColumns
            .map((c) => resolveCellText((row as any)[c.field]))
            .filter((s) => s !== '');
        return parts.length > 0 ? parts.join(' – ') : `#${(row as any).id}`;
    }, [labelColumns]);

    const selectedCount = selectedRowsMap.size;
    const multiSelectActive = !!multiSelectButtonTitle;

    return (
        <div>
            {multiSelectActive && (
                <div className="d-flex align-items-center flex-wrap gap-2 mb-2">
                    <DynamicButton
                        id="cancel"
                        title={translations.cancel || 'cancel'}
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
                    <Badge color="info" pill style={{ fontSize: '0.9rem' }}>
                        {(translations['multiselection.selectedCount'] || '{0} selected')
                            .replace('{0}', String(selectedCount))}
                    </Badge>
                    <DynamicButton
                        id="showSelected"
                        title={translations['multiselection.showSelected'] || 'Show selected'}
                        handleButtonClick={() => setShowSelectedModal(true)}
                        color="info"
                        outline
                        disabled={selectedCount === 0}
                    />
                    <DynamicButton
                        id="deselectAll"
                        title={translations['multiselection.deselectAll'] || 'Deselect all'}
                        handleButtonClick={deselectAll}
                        color="secondary"
                        outline
                        disabled={selectedCount === 0}
                    />
                </div>
            )}
            <DynamicTanStackGrid
                columnDefs={columnDefs}
                id={id}
                sortModel={sortModel}
                filterModel={filterModel}
                rowSelection={rowSelection}
                selectedEntities={selectedEntities}
                onSelectionChange={multiSelectActive ? handleSelectionChange : undefined}
                onGridApiReady={(table) => { tableRef.current = table; }}
                rowClickRedirectUrl={rowClickRedirectUrl}
                rowClickOpenModal={rowClickOpenModal}
                onColumnStatesChangedUrl={onColumnStatesChangedUrl}
                resetGridStateUrl={resetGridStateUrl}
                pagination={pagination}
                paginationPageSize={paginationPageSize}
                paginationPageSizeSelector={paginationPageSizeSelector}
                getRowClass={getRowClass}
            />
            <Modal isOpen={showSelectedModal} toggle={() => setShowSelectedModal(false)} size="lg">
                <ModalHeader toggle={() => setShowSelectedModal(false)}>
                    {(translations['multiselection.selected.title'] || 'Selected entries')}
                    {' '}
                    (
                    {selectedCount}
                    )
                </ModalHeader>
                <ModalBody>
                    {selectedCount === 0 ? (
                        <p className="text-muted mb-0">
                            {translations['multiselection.selected.empty'] || 'No entries selected.'}
                        </p>
                    ) : (
                        <ul className="list-group">
                            {Array.from(selectedRowsMap.entries()).map(([entityId, row]) => (
                                <li
                                    key={entityId}
                                    className="list-group-item d-flex justify-content-between align-items-center"
                                >
                                    <span className="text-truncate">{selectedLabel(row)}</span>
                                    <button
                                        type="button"
                                        className="btn btn-sm btn-outline-danger ms-2"
                                        onClick={() => deselectOne(entityId)}
                                        aria-label={translations['multiselection.deselectAll'] || 'Deselect'}
                                        title={translations['multiselection.deselectAll'] || 'Deselect'}
                                    >
                                        <i className="fas fa-times" />
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </ModalBody>
            </Modal>
        </div>
    );
}

export default DynamicListPageTanStackGrid;
