import type { FormatContext } from "@/lib/format";

/**
 * How a cell renders its value. Deliberately semantic rather than named after
 * the backend's AG-Grid vocabulary — the translation from `cellRenderer` /
 * `cellRendererParams.dataType` happens in `lib/dynamic/grid/cell-spec.ts`.
 */
export type CellKind =
  | "text"
  | "boolean"
  | "rating"
  | "consumption"
  | "tree"
  | "icon"
  | "orders"
  | "taskStatus";

/** The icon names a backend column def may map a value onto (UIIconType). */
export type CellIconName =
  | "checked"
  | "starRegular"
  | "userLock"
  | "paperClip"
  | "info"
  | "times";

/**
 * The render instruction for one column, stored in `ColumnMeta.cellSpec`.
 * Serialisable on purpose: it is derived from the layout response and must
 * survive memoisation without holding any React identity.
 */
export interface CellSpec {
  kind: CellKind;
  /** Formatter name for `kind: "text"`, e.g. "CURRENCY" (see FormatterName). */
  format?: string;
  /** Dot path to a sibling field holding the cell's tooltip (tooltipField). */
  tooltipPath?: string;
  /** For `kind: "icon"`: the value (as string) to icon name mapping. */
  valueIcons?: Record<string, CellIconName>;
  align?: "left" | "right";
}

export interface CellRenderProps {
  spec: CellSpec;
  value: unknown;
  /** The whole row, for specs that read a second field (tooltip, consumption). */
  row: Record<string, unknown>;
  ctx: FormatContext;
  /** Localised texts a cell needs for its accessible name. */
  t: (key: string) => string;
  /**
   * Whether a cell may link out of the table (Wicket's `linkEnabled`). Off where following the link
   * would leave what the user is doing — a select popover picks a task for a form, so the
   * consumption bar there is a picture and not a way to the task's time sheets.
   *
   * Default: on, as on the tree page and in the task list.
   */
  linkEnabled?: boolean;
  /**
   * The active search term, to highlight where it matched in the cell's text (see HighlightedText).
   * Only set where the client knows the term matched this text as a plain substring — the task tree,
   * whose backend filters with `containsIgnoreCase`. Left unset on the MagicFilter list pages, whose
   * query syntax (`field:value`, quotes, `*`, `-`) a substring highlight would mismark.
   */
  highlight?: string;
}
