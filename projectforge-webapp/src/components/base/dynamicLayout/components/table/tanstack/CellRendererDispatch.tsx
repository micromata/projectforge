import React, { Suspense, lazy } from 'react';
import { Cell, flexRender } from '@tanstack/react-table';
import Formatter from '../../../../Formatter';
import DynamicAgGridDiffCell from '../DynamicAgGridDiffCell';
import ImportStatusCell from '../ImportStatusCell';
import MultilineCell from '../MultilineCell';
import OpenModalLinkCell from '../../customized/components/OpenModalLinkCell';

const DynamicAgGridCustomizedCell = lazy(() => import('../DynamicAgGridCustomizedCell'));

interface CellRendererDispatchProps {
    cell: Cell<Record<string, unknown>, unknown>;
}

export default function CellRendererDispatch({ cell }: CellRendererDispatchProps) {
    const meta = cell.column.columnDef.meta as Record<string, any> | undefined;
    const renderer = meta?.cellRenderer;
    const params = meta?.cellRendererParams || {};
    const value = cell.getValue();
    const data = cell.row.original;
    const field = meta?.field || cell.column.id;

    if (!renderer) {
        if (value == null) return null;
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
