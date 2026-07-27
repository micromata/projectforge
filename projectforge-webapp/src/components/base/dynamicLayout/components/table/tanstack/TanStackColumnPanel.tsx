import React, { useState } from 'react';
import { Table } from '@tanstack/react-table';
import { getServiceURL } from '../../../../../../utilities/rest';

interface TanStackColumnPanelProps {
    table: Table<Record<string, unknown>>;
    resetGridStateUrl?: string;
    onReset?: (response: unknown) => void;
}

export default function TanStackColumnPanel({
    table,
    resetGridStateUrl,
    onReset,
}: TanStackColumnPanelProps) {
    const [open, setOpen] = useState(false);

    const allColumns = table.getAllLeafColumns();

    const handleReset = () => {
        if (!resetGridStateUrl) return;
        fetch(getServiceURL(resetGridStateUrl), {
            method: 'GET',
            credentials: 'include',
        })
            .then((response) => response.json())
            .then((data) => {
                if (onReset) {
                    onReset(data);
                }
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
        <div className="card card-body p-2 mb-2" style={{ maxWidth: 300 }}>
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
                {allColumns.map((column) => (
                    <div key={column.id} className="form-check">
                        <input
                            className="form-check-input"
                            type="checkbox"
                            id={`col-vis-${column.id}`}
                            checked={column.getIsVisible()}
                            onChange={column.getToggleVisibilityHandler()}
                        />
                        <label
                            className="form-check-label small"
                            htmlFor={`col-vis-${column.id}`}
                        >
                            {typeof column.columnDef.header === 'string'
                                ? column.columnDef.header
                                : column.id}
                        </label>
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
