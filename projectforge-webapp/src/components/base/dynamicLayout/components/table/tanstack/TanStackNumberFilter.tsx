import React, { useCallback, useContext, useState } from 'react';
import { Column } from '@tanstack/react-table';
import { DynamicLayoutContext } from '../../../context';

type Operator = 'equals' | 'notEqual' | 'greaterThan' | 'lessThan' | 'between' | 'blank' | 'notBlank';

export interface NumberFilterValue {
    type: 'number';
    operator: Operator;
    value?: number;
    valueTo?: number;
}

interface TanStackNumberFilterProps {
    column: Column<Record<string, unknown>, unknown>;
    onClose: () => void;
}

export default function TanStackNumberFilter({ column, onClose }: TanStackNumberFilterProps) {
    const { ui } = useContext(DynamicLayoutContext);
    const t = (key: string, fallback: string) => (ui as any)?.translations?.[key] || fallback;

    const OPERATORS: { value: Operator; label: string }[] = [
        { value: 'equals', label: t('filter.equals', 'Equals') },
        { value: 'notEqual', label: t('filter.notEqual', 'Not equal') },
        { value: 'greaterThan', label: t('filter.greaterThan', 'Greater than') },
        { value: 'lessThan', label: t('filter.lessThan', 'Less than') },
        { value: 'between', label: t('filter.between', 'Between') },
        { value: 'blank', label: t('filter.blank', 'Blank') },
        { value: 'notBlank', label: t('filter.notBlank', 'Not blank') },
    ];

    const current = column.getFilterValue() as NumberFilterValue | undefined;

    const [operator, setOperator] = useState<Operator>(current?.operator || 'equals');
    const [value, setValue] = useState<string>(current?.value != null ? String(current.value) : '');
    const [valueTo, setValueTo] = useState<string>(current?.valueTo != null ? String(current.valueTo) : '');

    const needsValue = operator !== 'blank' && operator !== 'notBlank';
    const needsSecondValue = operator === 'between';

    const apply = useCallback(() => {
        if (!needsValue) {
            column.setFilterValue({ type: 'number', operator } as NumberFilterValue);
        } else {
            const num = parseFloat(value);
            if (isNaN(num)) {
                column.setFilterValue(undefined);
            } else {
                const filter: NumberFilterValue = { type: 'number', operator, value: num };
                if (needsSecondValue) {
                    filter.valueTo = parseFloat(valueTo);
                }
                column.setFilterValue(filter);
            }
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
            style={{ minWidth: 200 }}
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
                        type="number"
                        className="form-control form-control-sm mb-2"
                        placeholder={t('filter.value', 'Value')}
                        value={value}
                        onChange={(e) => setValue(e.target.value)}
                        ref={(el) => el?.focus({ preventScroll: true })}
                    />
                )}
                {needsSecondValue && (
                    <input
                        type="number"
                        className="form-control form-control-sm mb-2"
                        placeholder={t('filter.valueTo', 'To')}
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
