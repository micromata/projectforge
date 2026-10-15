import React, { useCallback, useContext, useState } from 'react';
import moment from 'moment';
import 'react-day-picker/src/style.css';
import { Column } from '@tanstack/react-table';
import { DynamicLayoutContext } from '../../../context';
import DateInput from '../../../../../design/input/calendar/DateInput';

const ISO_DATE = 'YYYY-MM-DD';

// Convert stored filter string (YYYY-MM-DD) to a Date for DateInput.
const strToDate = (str?: string): Date | undefined => {
    if (!str) return undefined;
    const m = moment(str, ISO_DATE, true);
    return m.isValid() ? m.toDate() : undefined;
};

// Convert a Date from DateInput back to the stored filter string (YYYY-MM-DD).
const dateToStr = (date?: Date): string => (date ? moment(date).format(ISO_DATE) : '');

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

    const [operator, setOperator] = useState<Operator>(current?.operator || 'after');
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

    // Commit directly from an Enter press: the passed date is used instead of the
    // (not yet flushed) value state. Only for single-value operators.
    const applyEnter = useCallback((date: Date) => {
        if (!needsValue || needsSecondValue) return;
        const str = dateToStr(date);
        if (!str) {
            column.setFilterValue(undefined);
        } else {
            column.setFilterValue({ type: 'date', operator, value: str } as DateFilterValue);
        }
        onClose();
    }, [column, operator, needsValue, needsSecondValue, onClose]);

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
                    <div className="form-control form-control-sm mb-2 d-flex align-items-center">
                        <DateInput
                            value={strToDate(value)}
                            setDate={(d?: Date) => setValue(dateToStr(d))}
                            onEnter={applyEnter}
                            noInputContainer
                        />
                    </div>
                )}
                {needsSecondValue && (
                    <div className="form-control form-control-sm mb-2 d-flex align-items-center">
                        <DateInput
                            value={strToDate(valueTo)}
                            setDate={(d?: Date) => setValueTo(dateToStr(d))}
                            noInputContainer
                        />
                    </div>
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
