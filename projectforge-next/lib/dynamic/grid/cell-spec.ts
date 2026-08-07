import type {
  CellIconName,
  CellSpec,
} from "@/components/data-table/cells/cell-types";
import { iconNameFromWire } from "@/components/data-table/cells/cell-icons";
import { formatterToCellKind } from "@/lib/format-names";
import type { AgGridColumnDef } from "./ag-grid-types";

/**
 * Cell renderers of the backend that next doesn't implement yet. They fall back
 * to plain text, which is readable but incomplete — so say so once per name.
 *
 * `customized` (the address list's image preview, phone numbers and edit icon),
 * `diffCell` and `importStatusCell` all carry an `onClick` as JavaScript source,
 * which is why porting them means hand-writing the cell rather than adapting it.
 */
const DEFERRED_RENDERERS = new Set([
  "customized",
  "diffCell",
  "importStatusCell",
  "multilineCell",
]);

const warned = new Set<string>();

/**
 * Derives the render instruction for one column.
 *
 * Precedence follows the backend (UIAgGridColumnDef.createCol): a non-empty
 * `valueIconMap` makes the column an icon column and suppresses the formatter —
 * the backend sets `cellRenderer = "formatter"` for both cases, so the map has to
 * be checked first.
 */
export function cellSpecFor(col: AgGridColumnDef): CellSpec {
  const align =
    col.type === "numericColumn" || col.type === "rightAligned"
      ? "right"
      : undefined;
  const base = { tooltipPath: col.tooltipField, align } as const;

  const valueIcons = valueIconsOf(col);
  if (valueIcons) return { kind: "icon", valueIcons, ...base };

  if (col.cellRenderer === "formatter" || !col.cellRenderer) {
    const format = col.cellRendererParams?.dataType;
    return { kind: formatterToCellKind(format), format, ...base };
  }

  warnDeferred(col);
  return { kind: "text", format: col.cellRendererParams?.dataType, ...base };
}

/** Normalises `valueIconMap` to our own icon names, dropping unmapped values. */
function valueIconsOf(
  col: AgGridColumnDef
): Record<string, CellIconName> | undefined {
  const map = col.cellRendererParams?.valueIconMap;
  if (!map) return undefined;
  const icons: Record<string, CellIconName> = {};
  for (const [value, icon] of Object.entries(map)) {
    const name = iconNameFromWire(icon);
    // A null entry is the backend's way of saying "no icon for this value".
    if (name) icons[value] = name;
  }
  return Object.keys(icons).length > 0 ? icons : undefined;
}

function warnDeferred(col: AgGridColumnDef): void {
  const renderer = col.cellRenderer;
  if (!renderer || process.env.NODE_ENV === "production") return;
  const key = `${renderer}:${col.field ?? ""}`;
  if (warned.has(key)) return;
  warned.add(key);
  const known = DEFERRED_RENDERERS.has(renderer) ? "not ported yet" : "unknown";
  console.warn(
    `[dynamic-grid] cellRenderer "${renderer}" (${known}) on column "${col.field}" falls back to plain text.`
  );
}
