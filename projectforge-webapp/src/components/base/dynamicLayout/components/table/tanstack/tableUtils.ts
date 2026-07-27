import { ColumnDef, SortingState } from '@tanstack/react-table';

export interface DataTableColumnDef {
    field: string;
    headerName?: string;
    sortable?: boolean;
    resizable?: boolean;
    width?: number;
    minWidth?: number;
    maxWidth?: number;
    hide?: boolean;
    pinned?: 'left' | 'right' | null;
    filter?: string;
    filterParams?: Record<string, unknown>;
    cellRenderer?: string;
    cellRendererParams?: Record<string, unknown>;
    dataType?: string;
    formatter?: string;
    valueGetter?: string;
    valueFormatter?: string;
    valueIconMap?: Record<string, string[]>;
    type?: string;
    headerTooltip?: string;
    headerClass?: string[];
    wrapText?: boolean;
    autoHeight?: boolean;
    tooltipField?: string;
}

export function resolveNestedValue(row: Record<string, unknown>, fieldPath: string): unknown {
    if (!fieldPath || !row) return undefined;
    return fieldPath.split('.').reduce<unknown>((obj, key) => {
        if (obj == null) return undefined;
        return (obj as Record<string, unknown>)[key];
    }, row);
}

/**
 * Evaluates a valueGetter string like "data?.lendOutBy?.displayName"
 * by extracting the dot path and resolving it against the row.
 */
function evaluateValueGetter(valueGetter: string, row: Record<string, unknown>): unknown {
    // Strip "data?." or "data." prefix, then replace "?." with "."
    const path = valueGetter
        .replace(/^data\??\./, '')
        .replace(/\?\./g, '.');
    return resolveNestedValue(row, path);
}

export function buildColumnDefs(columns: DataTableColumnDef[]): ColumnDef<Record<string, unknown>>[] {
    return columns.map((col) => ({
        id: col.field,
        accessorFn: (row: Record<string, unknown>) => {
            if (col.valueGetter) {
                return evaluateValueGetter(col.valueGetter, row);
            }
            return resolveNestedValue(row, col.field);
        },
        header: col.headerName || col.field,
        size: col.width || 150,
        minSize: col.minWidth || 50,
        maxSize: col.maxWidth,
        enableSorting: col.sortable !== false,
        enableResizing: col.resizable !== false,
        enableHiding: true,
        meta: {
            field: col.field,
            cellRenderer: col.cellRenderer,
            cellRendererParams: col.cellRendererParams,
            dataType: col.dataType,
            formatter: col.formatter,
            valueIconMap: col.valueIconMap,
            filter: col.filter,
            filterParams: col.filterParams,
            pinned: col.pinned,
            headerClass: col.headerClass,
            headerTooltip: col.headerTooltip,
            tooltipField: col.tooltipField,
            wrapText: col.wrapText,
            autoHeight: col.autoHeight,
        },
    }));
}

export interface TableState {
    columnOrder: string[];
    columnSizing: Record<string, number>;
    columnVisibility: Record<string, boolean>;
    columnPinning: { left?: string[]; right?: string[] };
    sorting: SortingState;
    columnFilters: Array<{ id: string; value: unknown }>;
}

export function buildTableState(
    columnOrder: string[],
    columnSizing: Record<string, number>,
    columnVisibility: Record<string, boolean>,
    columnPinning: { left?: string[]; right?: string[] },
    sorting: SortingState,
    columnFilters: Array<{ id: string; value: unknown }>,
): TableState {
    return {
        columnOrder,
        columnSizing,
        columnVisibility,
        columnPinning,
        sorting,
        columnFilters,
    };
}

export function modifyRedirectUrl(redirectUrl: string, row: Record<string, unknown>): string {
    let url = redirectUrl;
    if (!row) return url;

    const idValue = row.id == null ? 'undefined' : String(row.id);
    url = url.replace('{id}', idValue);
    url = url.replace(':id', idValue);
    url = url.replace(/\/id(?=[?/]|$)/g, `/${idValue}`);

    Object.keys(row).forEach((fieldName) => {
        if (fieldName === 'id') return;
        const fieldValue = row[fieldName] == null ? 'undefined' : String(row[fieldName]);
        url = url.replace(`{${fieldName}}`, fieldValue);
        url = url.replace(`:${fieldName}`, fieldValue);
        const pathSegmentPattern = new RegExp(`/${fieldName}(?=[?/]|$)`, 'g');
        url = url.replace(pathSegmentPattern, `/${fieldValue}`);
        const queryParamPattern = new RegExp(`${fieldName}=${fieldName}(?=&|$)`, 'g');
        url = url.replace(queryParamPattern, `${fieldName}=${fieldValue}`);
    });

    return url;
}
