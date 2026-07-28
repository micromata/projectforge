import React, { useCallback, useContext, useState } from 'react';
import { Column } from '@tanstack/react-table';
import { DynamicLayoutContext } from '../../../context';

type Operator = 'equals' | 'notEqual' | 'before' | 'after' | 'between' | 'blank' | 'notBlank';

export interface DateFilterValue {
    type: 'date';
    operator: Operator;
    value?: string;
    valueTo?: string;
}

interface TanStackDateFilterProps {
    column: Column<Record<string, unknown>, unknown>;
    onClose: () => void;
}

export default function TanStackDateFilter({ column, onClose }: TanStackDateFilterProps) {
    const { ui } = useContext(DynamicLayoutContext);
    const t = (key: string, fallback: string) => (ui as any)?.translations?.[key] || fallback;

    const OPERATORS: { value: Operator; label: string }[] = [
        { value: 'equals', label: t('filter.equals', 'Equals') },
        { value: 'notEqual', label: t('filter.notEqual', 'Not equal') },
        { value: 'before', label: t('filter.before', 'Before') },
        { value: 'after', label: t('filter.after', 'After') },
        { value: 'between', label: t('filter.between', 'Between') },
        { value: 'blank', label: t('filter.blank', 'Blank') },
        { value: 'notBlank', label: t('filter.notBlank', 'Not blank') },
    ];

    const current = column.getFilterValue() as DateFilterValue | undefined;

    const [operator, setOperator] = useState<Operator>(current?.operator || 'equals');
    const [value, setValue] = useState<string>(current?.value || '');
    const [valueTo, setValueTo] = useState<string>(current?.valueTo || '');

    const needsValue = operator !== 'blank' && operator !== 'notBlank';
    const needsSecondValue = operator === 'between';

    const apply = useCallback(() => {
        if (!needsValue) {
            column.setFilterValue({ type: 'date', operator } as DateFilterValue);
        } else if (!value) {
            column.setFilterValue(undefined);
        } else {
            const filter: DateFilterValue = { type: 'date', operator, value };
            if (needsSecondValue) {
                filter.valueTo = valueTo;
            }
            column.setFilterValue(filter);
        }
        onClose();
    }, [column, operator, value, valueTo, needsValue, needsSecondValue, onClose]);

    const reset = useCallback(() => {
        column.setFilterValue(undefined);
        onClose();
    }, [column, onClose]);

    return (
        <div
            className="card shadow"
            style={{ minWidth: 220 }}
            onClick={(e) => e.stopPropagation()}
        >
            <div className="card-body p-2">
                <select
                    className="form-select form-select-sm mb-2"
                    value={operator}
                    onChange={(e) => setOperator(e.target.value as Operator)}
                >
                    {OPERATORS.map((op) => (
                        <option key={op.value} value={op.value}>{op.label}</option>
                    ))}
                </select>
                {needsValue && (
                    <input
                        type="date"
                        className="form-control form-control-sm mb-2"
                        value={value}
                        onChange={(e) => setValue(e.target.value)}
                        ref={(el) => el?.focus({ preventScroll: true })}
                    />
                )}
                {needsSecondValue && (
                    <input
                        type="date"
                        className="form-control form-control-sm mb-2"
                        value={valueTo}
                        onChange={(e) => setValueTo(e.target.value)}
                    />
                )}
                <div className="d-flex justify-content-between">
                    <button type="button" className="btn btn-sm btn-outline-secondary" onClick={reset}>
                        {t('filter.reset', 'Reset')}
                    </button>
                    <button type="button" className="btn btn-sm btn-primary" onClick={apply}>
                        {t('filter.apply', 'Apply')}
                    </button>
                </div>
            </div>
        </div>
    );
}
