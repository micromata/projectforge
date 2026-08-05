import React, { Suspense, lazy } from 'react';
import { Cell, flexRender } from '@tanstack/react-table';
import { evaluateFieldExpression } from './tableUtils';
import Formatter from '../../../../Formatter';
import DynamicAgGridDiffCell from '../DynamicAgGridDiffCell';
import ImportStatusCell from '../ImportStatusCell';
import MultilineCell from '../MultilineCell';
import OpenModalLinkCell from '../../customized/components/OpenModalLinkCell';

const DynamicAgGridCustomizedCell = lazy(() => import('../DynamicAgGridCustomizedCell'));

interface CellRendererDispatchProps {
    cell: Cell<Record<string, unknown>, unknown>;
    // Custom renderer components keyed by cellRenderer name (e.g. action, filename), supplied by
    // the grid's parent. Rendered with AG-Grid-style params for backwards compatibility.
    components?: Record<string, React.ComponentType<any>>;
}

export default function CellRendererDispatch({ cell, components }: CellRendererDispatchProps) {
    const meta = cell.column.columnDef.meta as Record<string, any> | undefined;
    const renderer = meta?.cellRenderer;
    const params = meta?.cellRendererParams || {};
    const value = cell.getValue();
    const data = cell.row.original;
    const field = meta?.field || cell.column.id;
    const valueFormatter = meta?.valueFormatter as string | undefined;

    // Custom component renderers provided by the parent (e.g. attachment "action"/"filename").
    // These mirror the former AG-Grid framework components and expect AG-Grid-style params.
    if (renderer && components && components[renderer]) {
        const Component = components[renderer];
        return (
            <Component
                data={data}
                value={value}
                colDef={{ field, cellRendererParams: params }}
                {...params}
            />
        );
    }

    if (!renderer) {
        // valueFormatter is a field expression like "data.sizeHumanReadable" — display that
        // formatted field of the row instead of the raw cell value (e.g. "1 MB" instead of bytes).
        if (valueFormatter) {
            const formatted = evaluateFieldExpression(valueFormatter, data as Record<string, unknown>);
            return formatted == null ? null : <>{String(formatted)}</>;
        }
        if (value == null) return null;
        // Handle objects with displayName (e.g. User objects)
        if (typeof value === 'object') {
            if (Array.isArray(value)) {
                const names = value.map((item: any) => item?.displayName ?? String(item)).join(', ');
                return <>{names}</>;
            }
            if ((value as any).displayName) {
                return <>{(value as any).displayName}</>;
            }
            return null;
        }
        return <>{String(value)}</>;
    }

    switch (renderer) {
        case 'formatter':
            return (
                <Formatter
                    value={value}
                    data={data}
                    id={field}
                    dataType={params.dataType || meta?.dataType}
                    formatter={params.formatter || meta?.formatter}
                    valueIconMap={params.valueIconMap || meta?.valueIconMap}
                    {...params}
                />
            );
        case 'diffCell':
            return (
                <DynamicAgGridDiffCell
                    value={value}
                    data={data}
                    colDef={{ field, cellRendererParams: params }}
                />
            );
        case 'customized':
            return (
                <Suspense fallback={<span>...</span>}>
                    <DynamicAgGridCustomizedCell
                        data={data}
                        colDef={{ field, cellRendererParams: params }}
                    />
                </Suspense>
            );
        case 'importStatusCell':
            return <ImportStatusCell value={value as string} data={data} />;
        case 'multilineCell':
            return <MultilineCell value={value as string} />;
        case 'OpenModalLinkCell':
            return (
                <OpenModalLinkCell
                    value={value as any}
                    data={data as any}
                    urlPattern={params.urlPattern}
                    multiline={params.multiline}
                    placeholder={params.placeholder}
                />
            );
        default:
            return <>{flexRender(cell.column.columnDef.cell, cell.getContext())}</>;
    }
}
