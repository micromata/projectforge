import React, { useEffect, useRef, useState } from 'react';
import { Table } from '@tanstack/react-table';
import { getServiceURL } from '../../../../../../utilities/rest';

interface TanStackColumnPanelProps {
    table: Table<Record<string, unknown>>;
    columnOrder: string[];
    onColumnOrderChange: (newOrder: string[]) => void;
    resetGridStateUrl?: string;
    onReset?: (response: unknown) => void;
}

export default function TanStackColumnPanel({
    table,
    columnOrder,
    onColumnOrderChange,
    resetGridStateUrl,
    onReset,
}: TanStackColumnPanelProps) {
    const [open, setOpen] = useState(false);
    const panelRef = useRef<HTMLDivElement>(null);
    const draggedId = useRef<string | null>(null);
    const [dragOverId, setDragOverId] = useState<string | null>(null);

    const allColumns = table.getAllLeafColumns();
    const pinnedLeft = table.getState().columnPinning.left || [];

    // Split into pinned (fixed, not reorderable) and unpinned (reorderable)
    const pinnedColumns = allColumns.filter((c) => pinnedLeft.includes(c.id));
    const unpinnedColumns = [...allColumns]
        .filter((c) => !pinnedLeft.includes(c.id))
        .sort((a, b) => {
            const ai = columnOrder.indexOf(a.id);
            const bi = columnOrder.indexOf(b.id);
            if (ai === -1 && bi === -1) return 0;
            if (ai === -1) return 1;
            if (bi === -1) return -1;
            return ai - bi;
        });

    // Close on outside click or Escape
    useEffect(() => {
        if (!open) return;
        const handleClickOutside = (e: MouseEvent) => {
            if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        const handleEscape = (e: KeyboardEvent) => {
            if (e.key === 'Escape') setOpen(false);
        };
        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('keydown', handleEscape);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('keydown', handleEscape);
        };
    }, [open]);

    const handleDragStart = (columnId: string) => {
        draggedId.current = columnId;
    };

    const handleDragOver = (e: React.DragEvent, columnId: string) => {
        e.preventDefault();
        setDragOverId(columnId);
    };

    const handleDrop = (targetId: string) => {
        const sourceId = draggedId.current;
        if (!sourceId || sourceId === targetId) {
            draggedId.current = null;
            setDragOverId(null);
            return;
        }
        const newOrder = [...columnOrder];
        const fromIdx = newOrder.indexOf(sourceId);
        const toIdx = newOrder.indexOf(targetId);
        if (fromIdx !== -1 && toIdx !== -1) {
            newOrder.splice(fromIdx, 1);
            newOrder.splice(toIdx, 0, sourceId);
            onColumnOrderChange(newOrder);
        }
        draggedId.current = null;
        setDragOverId(null);
    };

    const handleDragEnd = () => {
        draggedId.current = null;
        setDragOverId(null);
    };

    const handleTogglePin = (columnId: string) => {
        const current = table.getState().columnPinning;
        const left = current.left || [];
        if (left.includes(columnId)) {
            table.setColumnPinning({ ...current, left: left.filter((id) => id !== columnId) });
        } else {
            table.setColumnPinning({ ...current, left: [...left, columnId] });
        }
    };

    const handleReset = () => {
        if (!resetGridStateUrl) return;
        fetch(getServiceURL(resetGridStateUrl), {
            method: 'GET',
            credentials: 'include',
        })
            .then((response) => response.json())
            .then((data) => {
                if (onReset) onReset(data);
            })
            .catch((error) => {
                console.error('Error resetting grid state:', error);
            });
    };

    if (!open) {
        return (
            <button
                type="button"
                className="btn btn-sm btn-outline-secondary mb-2"
                onClick={() => setOpen(true)}
                title="Spalten verwalten"
            >
                <i className="fas fa-columns" />
            </button>
        );
    }

    return (
        <div ref={panelRef} className="card card-body p-2 mb-2" style={{ maxWidth: 320 }}>
            <div className="d-flex justify-content-between align-items-center mb-2">
                <strong className="small">Spalten</strong>
                <button
                    type="button"
                    className="btn-close btn-close-sm"
                    onClick={() => setOpen(false)}
                    aria-label="Schließen"
                />
            </div>
            <div style={{ maxHeight: 300, overflowY: 'auto' }}>
                {/* Pinned columns: fixed, not draggable */}
                {pinnedColumns.map((column) => (
                    <div key={column.id} className="d-flex align-items-center gap-1 py-1 opacity-50">
                        <span className="text-muted" style={{ padding: '0 4px' }}>
                            <i className="fas fa-lock" style={{ fontSize: '0.65rem' }} />
                        </span>
                        <input
                            className="form-check-input"
                            type="checkbox"
                            id={`col-vis-${column.id}`}
                            checked={column.getIsVisible()}
                            onChange={column.getToggleVisibilityHandler()}
                        />
                        <label className="form-check-label small flex-grow-1" htmlFor={`col-vis-${column.id}`}>
                            {typeof column.columnDef.header === 'string'
                                ? column.columnDef.header
                                : column.id}
                        </label>
                        <button
                            type="button"
                            className="btn btn-sm p-0 text-primary"
                            onClick={() => handleTogglePin(column.id)}
                            title="Nicht mehr anheften"
                            style={{ fontSize: '0.75rem', lineHeight: 1 }}
                        >
                            <i className="fas fa-thumbtack" />
                        </button>
                    </div>
                ))}
                {/* Unpinned columns: draggable */}
                {unpinnedColumns.map((column) => (
                    <div
                        key={column.id}
                        className={`d-flex align-items-center gap-1 py-1${dragOverId === column.id ? ' bg-light' : ''}`}
                        draggable
                        onDragStart={() => handleDragStart(column.id)}
                        onDragOver={(e) => handleDragOver(e, column.id)}
                        onDrop={() => handleDrop(column.id)}
                        onDragEnd={handleDragEnd}
                    >
                        <span
                            className="text-muted"
                            style={{ cursor: 'grab', padding: '0 4px' }}
                            title="Ziehen zum Sortieren"
                        >
                            ⠿
                        </span>
                        <input
                            className="form-check-input"
                            type="checkbox"
                            id={`col-vis-${column.id}`}
                            checked={column.getIsVisible()}
                            onChange={column.getToggleVisibilityHandler()}
                        />
                        <label className="form-check-label small flex-grow-1" htmlFor={`col-vis-${column.id}`}>
                            {typeof column.columnDef.header === 'string'
                                ? column.columnDef.header
                                : column.id}
                        </label>
                        <button
                            type="button"
                            className="btn btn-sm p-0 text-muted"
                            onClick={() => handleTogglePin(column.id)}
                            title="Links anheften"
                            style={{ fontSize: '0.75rem', lineHeight: 1 }}
                        >
                            <i className="fas fa-thumbtack" />
                        </button>
                    </div>
                ))}
            </div>
            {resetGridStateUrl && (
                <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary mt-2"
                    onClick={handleReset}
                >
                    Spalten zurücksetzen
                </button>
            )}
        </div>
    );
}
