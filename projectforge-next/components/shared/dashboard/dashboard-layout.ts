import type {
  DashboardTileHeight,
  DashboardTileLayout,
  DashboardTileWidth,
} from "@/lib/rs/ui-settings";

/** What a page declares about one of its tiles; the user's layout overrides order, size and visibility. */
export interface DashboardTileDefaults {
  id: string;
  defaultWidth?: DashboardTileWidth;
  defaultHeight?: DashboardTileHeight;
}

/** A tile as rendered: every field resolved. */
export interface ResolvedTile {
  id: string;
  width: DashboardTileWidth;
  height: DashboardTileHeight;
  hidden: boolean;
}

/** The fields of a tile an edit may change. */
export type TilePatch = Partial<Omit<ResolvedTile, "id">>;

export const TILE_WIDTHS: DashboardTileWidth[] = [
  "third",
  "half",
  "twoThirds",
  "full",
];
export const TILE_HEIGHTS: DashboardTileHeight[] = ["S", "M", "L"];

/**
 * The tiles in display order: the stored layout for the ids the page still declares, then the declared
 * tiles the layout doesn't know yet (added after the user arranged the dashboard) in their declared order.
 * Stored ids the page no longer declares are dropped, so tiles can come and go without a migration — the
 * same idea as the list's column order (see column-order.ts).
 */
export function resolveTiles(
  defaults: DashboardTileDefaults[],
  stored: DashboardTileLayout[] | undefined
): ResolvedTile[] {
  const byId = new Map(defaults.map((tile) => [tile.id, tile]));
  const seen = new Set<string>();
  const result: ResolvedTile[] = [];
  for (const entry of stored ?? []) {
    const tile = byId.get(entry.id);
    if (!tile || seen.has(entry.id)) continue;
    seen.add(entry.id);
    result.push({
      id: entry.id,
      width:
        entry.width && TILE_WIDTHS.includes(entry.width)
          ? entry.width
          : defaultWidth(tile),
      height:
        entry.height && TILE_HEIGHTS.includes(entry.height)
          ? entry.height
          : defaultHeight(tile),
      hidden: entry.hidden === true,
    });
  }
  for (const tile of defaults) {
    if (seen.has(tile.id)) continue;
    result.push({
      id: tile.id,
      width: defaultWidth(tile),
      height: defaultHeight(tile),
      hidden: false,
    });
  }
  return result;
}

/** Whether the resolved tiles are exactly the defaults — then nothing needs to be stored (an empty layout). */
export function isDefaultLayout(
  defaults: DashboardTileDefaults[],
  tiles: ResolvedTile[]
): boolean {
  return (
    JSON.stringify(resolveTiles(defaults, undefined)) === JSON.stringify(tiles)
  );
}

/** Moves the tile `activeId` to the slot of `overId`, as dnd-kit's arrayMove does. */
export function moveTile(
  tiles: ResolvedTile[],
  activeId: string,
  overId: string
): ResolvedTile[] {
  const from = tiles.findIndex((tile) => tile.id === activeId);
  const to = tiles.findIndex((tile) => tile.id === overId);
  if (from < 0 || to < 0 || from === to) return tiles;
  const next = [...tiles];
  const [moved] = next.splice(from, 1);
  next.splice(to, 0, moved);
  return next;
}

/** Replaces the fields of tile `id`. */
export function updateTile(
  tiles: ResolvedTile[],
  id: string,
  patch: TilePatch
): ResolvedTile[] {
  return tiles.map((tile) => (tile.id === id ? { ...tile, ...patch } : tile));
}

function defaultWidth(tile: DashboardTileDefaults): DashboardTileWidth {
  return tile.defaultWidth ?? "full";
}

function defaultHeight(tile: DashboardTileDefaults): DashboardTileHeight {
  return tile.defaultHeight ?? "M";
}
