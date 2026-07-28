import React, { useCallback, useContext, useMemo, useState } from 'react';
import { Column, Table } from '@tanstack/react-table';
import { DynamicLayoutContext } from '../../../context';

type Mode = 'selection' | 'contains' | 'notContains' | 'blank' | 'notBlank';

interface TanStackColumnFilterProps {
    column: Column<Record<string, unknown>, unknown>;
    table: Table<Record<string, unknown>>;
    onClose: () => void;
}

export default function TanStackColumnFilter({ column, table, onClose }: TanStackColumnFilterProps) {
    const { ui } = useContext(DynamicLayoutContext);
    const t = (key: string, fallback: string) => (ui as any)?.translations?.[key] || fallback;

    // Derive initial mode from current filter value
    const currentFilter = column.getFilterValue();
    const initialMode: Mode = useMemo(() => {
        if (!currentFilter) return 'selection';
        if (Array.isArray(currentFilter)) return 'selection';
        if (currentFilter && typeof currentFilter === 'object' && (currentFilter as any).type === 'text') {
            const op = (currentFilter as any).operator;
            if (op === 'contains') return 'contains';
            if (op === 'notContains') return 'notContains';
            if (op === 'blank') return 'blank';
            if (op === 'notBlank') return 'notBlank';
        }
        return 'selection';
    }, [currentFilter]);

    const [mode, setMode] = useState<Mode>(initialMode);
    const [textValue, setTextValue] = useState<string>(
        (currentFilter && typeof currentFilter === 'object' && (currentFilter as any).value) || '',
    );
    const [search, setSearch] = useState('');

    // Get all unique values for selection mode
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

    // Filter values by search in selection mode
    const filteredValues = useMemo(() => {
        if (!search) return allValues;
        const lower = search.toLowerCase();
        const blankLabel = t('filter.blank', 'Blank').toLowerCase();
        return allValues.filter((v) => (v === '' ? blankLabel : v.toLowerCase()).includes(lower));
    }, [allValues, search]);

    // Selection state
    const selectedValues = useMemo(() => {
        if (Array.isArray(currentFilter)) return new Set(currentFilter as string[]);
        return new Set(allValues);
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

    // Apply text/blank filter
    const applyTextFilter = useCallback(() => {
        if (mode === 'blank' || mode === 'notBlank') {
            column.setFilterValue({ type: 'text', operator: mode });
        } else if (mode === 'contains' || mode === 'notContains') {
            if (!textValue) {
                column.setFilterValue(undefined);
            } else {
                column.setFilterValue({ type: 'text', operator: mode, value: textValue });
            }
        }
        onClose();
    }, [column, mode, textValue, onClose]);

    const reset = useCallback(() => {
        column.setFilterValue(undefined);
        onClose();
    }, [column, onClose]);

    // When mode changes to blank/notBlank, apply immediately
    const handleModeChange = useCallback((newMode: Mode) => {
        setMode(newMode);
        if (newMode === 'selection') {
            // Restore to no filter (show all)
            column.setFilterValue(undefined);
        }
    }, [column]);

    const MODES: { value: Mode; label: string }[] = [
        { value: 'selection', label: t('filter.selection', 'Selection') },
        { value: 'contains', label: t('filter.contains', 'Contains') },
        { value: 'notContains', label: t('filter.notContains', 'Does not contain') },
        { value: 'blank', label: t('filter.blank', 'Blank') },
        { value: 'notBlank', label: t('filter.notBlank', 'Not blank') },
    ];

    return (
        <div
            className="card shadow"
            style={{ minWidth: 220, maxWidth: 320 }}
            onClick={(e) => e.stopPropagation()}
        >
            <div className="card-body p-2">
                <select
                    className="form-select form-select-sm mb-2"
                    value={mode}
                    onChange={(e) => handleModeChange(e.target.value as Mode)}
                >
                    {MODES.map((m) => (
                        <option key={m.value} value={m.value}>{m.label}</option>
                    ))}
                </select>

                {mode === 'selection' && (
                    <>
                        <input
                            type="text"
                            className="form-control form-control-sm mb-2"
                            placeholder={t('filter.search', 'Search...')}
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            ref={(el) => el?.focus({ preventScroll: true })}
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
                                {t('filter.selectAll', 'Select all')}
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
                                        {val === '' ? <em>({t('filter.blank', 'Blank')})</em> : val}
                                    </label>
                                </div>
                            ))}
                        </div>
                        <div className="d-flex justify-content-end mt-2">
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onClose}>
                                {t('filter.close', 'Close')}
                            </button>
                        </div>
                    </>
                )}

                {(mode === 'contains' || mode === 'notContains') && (
                    <>
                        <input
                            type="text"
                            className="form-control form-control-sm mb-2"
                            placeholder={t('filter.value', 'Value')}
                            value={textValue}
                            onChange={(e) => setTextValue(e.target.value)}
                            ref={(el) => el?.focus({ preventScroll: true })}
                        />
                        <div className="d-flex justify-content-between">
                            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={reset}>
                                {t('filter.reset', 'Reset')}
                            </button>
                            <button type="button" className="btn btn-sm btn-primary" onClick={applyTextFilter}>
                                {t('filter.apply', 'Apply')}
                            </button>
                        </div>
                    </>
                )}

                {(mode === 'blank' || mode === 'notBlank') && (
                    <div className="d-flex justify-content-between">
                        <button type="button" className="btn btn-sm btn-outline-secondary" onClick={reset}>
                            {t('filter.reset', 'Reset')}
                        </button>
                        <button type="button" className="btn btn-sm btn-primary" onClick={applyTextFilter}>
                            {t('filter.apply', 'Apply')}
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
