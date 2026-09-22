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
    /** Field path in the row's JSON used for sorting instead of the displayed value. */
    sortField?: string;
}

// ISO date / timestamp as produced by the backend (LocalDate: yyyy-MM-dd,
// java.util.Date: yyyy-MM-dd'T'HH:mm:ss.SSS'Z').
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2}(\.\d+)?Z?)?)?$/;

const displayNameOf = (v: unknown): string => (
    v != null && typeof v === 'object'
        ? String((v as Record<string, unknown>).displayName ?? '')
        : String(v ?? '')
);

/**
 * Reduces a cell value to a comparable primitive (number, string, or null for blank).
 * ISO date/timestamp strings are converted to epoch millis so chronological order is preserved
 * regardless of locale-specific display format.
 */
export function toSortPrimitive(value: unknown): number | string | null {
    if (value == null) return null;
    if (typeof value === 'number') return Number.isNaN(value) ? null : value;
    if (typeof value === 'boolean') return value ? 1 : 0;
    if (value instanceof Date) return value.getTime();
    if (Array.isArray(value)) {
        const s = value.map(displayNameOf).filter((x) => x !== '').join(', ');
        return s === '' ? null : s;
    }
    if (typeof value === 'object') {
        const s = displayNameOf(value).trim();
        return s === '' ? null : s;
    }
    const s = String(value).trim();
    if (s === '') return null;
    if (ISO_DATE_RE.test(s)) {
        const t = Date.parse(s);
        if (!Number.isNaN(t)) return t;
    }
    return s;
}

/**
 * Creates a locale-aware comparator using the given Intl.Collator.
 * Blank/null values always sort last in ascending order (TanStack negates for descending),
 * matching AG-Grid's former accentedSort behaviour.
 */
export function createValueComparator(collator: Intl.Collator): (a: unknown, b: unknown) => number {
    return (a: unknown, b: unknown): number => {
        const av = toSortPrimitive(a);
        const bv = toSortPrimitive(b);
        if (av === null || bv === null) {
            if (av === null && bv === null) return 0;
            return av === null ? 1 : -1;
        }
        if (typeof av === 'number' && typeof bv === 'number') {
            return av === bv ? 0 : (av < bv ? -1 : 1);
        }
        return collator.compare(String(av), String(bv));
    };
}

export function resolveNestedValue(row: Record<string, unknown>, fieldPath: string): unknown {
    if (!fieldPath || !row) return undefined;
    return fieldPath.split('.').reduce<unknown>((obj, key) => {
        if (obj == null) return undefined;
        return (obj as Record<string, unknown>)[key];
    }, row);
}

/**
 * Evaluates a valueGetter/valueFormatter string like "data?.lendOutBy?.displayName" or
 * "data.sizeHumanReadable" by extracting the dot path and resolving it against the row.
 */
export function evaluateFieldExpression(expression: string, row: Record<string, unknown>): unknown {
    // Strip "data?." or "data." prefix, then replace "?." with "."
    const path = expression
        .replace(/^data\??\./, '')
        .replace(/\?\./g, '.');
    return resolveNestedValue(row, path);
}

const DEFAULT_COLLATOR = new Intl.Collator(undefined, { numeric: true });
const DEFAULT_COMPARATOR = createValueComparator(DEFAULT_COLLATOR);

export function buildColumnDefs(
    columns: DataTableColumnDef[],
    compareValues: (a: unknown, b: unknown) => number = DEFAULT_COMPARATOR,
): ColumnDef<Record<string, unknown>>[] {
    return columns.map((col) => ({
        id: col.field,
        accessorFn: (row: Record<string, unknown>) => {
            if (col.valueGetter) {
                return evaluateFieldExpression(col.valueGetter, row);
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
        // Blank handling is done in compareValues; TanStack's default sortUndefined:1 would
        // short-circuit before sortingFn is called, inspecting the display value which is
        // unrelated for sortField columns.
        sortUndefined: false,
        sortingFn: (rowA, rowB, columnId) => {
            const sortField = col.sortField;
            const a = sortField
                ? evaluateFieldExpression(sortField, rowA.original)
                : rowA.getValue(columnId);
            const b = sortField
                ? evaluateFieldExpression(sortField, rowB.original)
                : rowB.getValue(columnId);
            return compareValues(a, b);
        },
        meta: {
            field: col.field,
            cellRenderer: col.cellRenderer,
            cellRendererParams: col.cellRendererParams,
            valueFormatter: col.valueFormatter,
            dataType: col.dataType,
            formatter: col.formatter,
            valueIconMap: col.valueIconMap,
            filter: col.filter,
            filterParams: col.filterParams,
            pinned: col.pinned,
            headerClass: col.headerClass,
            headerTooltip: col.headerTooltip,
            tooltipField: col.tooltipField,
            type: col.type,
            wrapText: col.wrapText,
            autoHeight: col.autoHeight,
            sortField: col.sortField,
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
