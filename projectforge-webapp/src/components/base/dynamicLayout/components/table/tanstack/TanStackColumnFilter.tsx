import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Column, Table } from '@tanstack/react-table';

interface TanStackColumnFilterProps {
    column: Column<Record<string, unknown>, unknown>;
    table: Table<Record<string, unknown>>;
    onClose: () => void;
}

export default function TanStackColumnFilter({ column, table, onClose }: TanStackColumnFilterProps) {
    const [search, setSearch] = useState('');
    const ref = useRef<HTMLDivElement>(null);

    // Close on outside click
    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                onClose();
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [onClose]);

    // Get all unique values for this column
    const allValues = useMemo(() => {
        const values = new Set<string>();
        table.getPreFilteredRowModel().rows.forEach((row) => {
            const val = row.getValue(column.id);
            if (val == null || val === '') {
                values.add('');
            } else if (typeof val === 'object') {
                if (Array.isArray(val)) {
                    val.forEach((item: any) => values.add(item?.displayName ?? String(item)));
                } else {
                    values.add((val as any).displayName ?? String(val));
                }
            } else {
                values.add(String(val));
            }
        });
        return Array.from(values).sort((a, b) => {
            if (a === '') return 1;
            if (b === '') return -1;
            return a.localeCompare(b);
        });
    }, [column.id, table]);

    // Filter values by search
    const filteredValues = useMemo(() => {
        if (!search) return allValues;
        const lower = search.toLowerCase();
        return allValues.filter((v) => (v === '' ? '(Leer)' : v).toLowerCase().includes(lower));
    }, [allValues, search]);

    // Current filter state: set of selected values (empty means all selected)
    const currentFilter = column.getFilterValue() as string[] | undefined;
    const selectedValues = useMemo(() => {
        if (!currentFilter) return new Set(allValues);
        return new Set(currentFilter);
    }, [currentFilter, allValues]);

    const allSelected = selectedValues.size === allValues.length;

    const toggleValue = useCallback((val: string) => {
        const newSet = new Set(selectedValues);
        if (newSet.has(val)) {
            newSet.delete(val);
        } else {
            newSet.add(val);
        }
        if (newSet.size === allValues.length) {
            column.setFilterValue(undefined);
        } else if (newSet.size === 0) {
            column.setFilterValue([]);
        } else {
            column.setFilterValue(Array.from(newSet));
        }
    }, [selectedValues, allValues, column]);

    const toggleAll = useCallback(() => {
        if (allSelected) {
            column.setFilterValue([]);
        } else {
            column.setFilterValue(undefined);
        }
    }, [allSelected, column]);

    return (
        <div
            ref={ref}
            className="card shadow position-absolute"
            style={{ zIndex: 1000, top: '100%', left: 0, minWidth: 200, maxWidth: 300 }}
            onClick={(e) => e.stopPropagation()}
        >
            <div className="card-body p-2">
                <input
                    type="text"
                    className="form-control form-control-sm mb-2"
                    placeholder="Suchen..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    autoFocus
                />
                <div className="form-check border-bottom pb-1 mb-1">
                    <input
                        className="form-check-input"
                        type="checkbox"
                        id={`filter-all-${column.id}`}
                        checked={allSelected}
                        onChange={toggleAll}
                    />
                    <label className="form-check-label small fw-bold" htmlFor={`filter-all-${column.id}`}>
                        Alles auswählen
                    </label>
                </div>
                <div style={{ maxHeight: 200, overflowY: 'auto' }}>
                    {filteredValues.map((val) => (
                        <div key={val} className="form-check">
                            <input
                                className="form-check-input"
                                type="checkbox"
                                id={`filter-${column.id}-${val}`}
                                checked={selectedValues.has(val)}
                                onChange={() => toggleValue(val)}
                            />
                            <label className="form-check-label small" htmlFor={`filter-${column.id}-${val}`}>
                                {val === '' ? <em>(Leer)</em> : val}
                            </label>
                        </div>
                    ))}
                </div>
                <div className="d-flex justify-content-end mt-2">
                    <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onClose}>
                        Schließen
                    </button>
                </div>
            </div>
        </div>
    );
}
