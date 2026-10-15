"use client";

import { useMemo, useState, type ReactNode } from "react";
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  rectSortingStrategy,
  sortableKeyboardCoordinates,
} from "@dnd-kit/sortable";
import { useTranslations } from "next-intl";
import type { DashboardTileLayout } from "@/lib/rs/ui-settings";
import { cn } from "@/lib/utils";
import {
  isDefaultLayout,
  moveTile,
  resolveTiles,
  updateTile,
  type DashboardTileDefaults,
  type ResolvedTile,
} from "./dashboard-layout";
import { DashboardTileCard, TILE_HEIGHT_CLASS } from "./dashboard-tile";
import { DashboardToolbar } from "./dashboard-toolbar";
import {
  useDashboardLayoutPersistence,
  useStoredDashboardLayout,
} from "./use-dashboard-layout";

/** One chart (or other block) of a dashboard. */
export interface DashboardTile extends DashboardTileDefaults {
  title: ReactNode;
  /**
   * The tile's content. `chartClassName` carries the height of the tile's size step and belongs on the
   * chart's `ChartContainer` (the charts keep their own default height outside a dashboard).
   */
  render: (chartClassName: string) => ReactNode;
  /** Content of its own height (e.g. key figures): the tile offers no height steps. */
  fixedHeight?: boolean;
}

/**
 * The charts of a page as tiles the user can arrange like a dashboard: reorder by drag and drop, choose a
 * width (⅓, ½, ⅔, full) and a height step, hide tiles and show them again. The layout is stored per user and
 * per dashboard `id` (see `UISettingsRest`), so it follows the user across devices.
 *
 * Editing is a mode ("Arrange"), so the charts can't be moved by accident while being read. Tiles the page
 * adds later appear with their defaults; tiles it drops vanish from the stored layout.
 */
export function ChartDashboard({
  id,
  tiles,
  className,
}: {
  /** Stable, unique id of the dashboard, e.g. `liquidity.forecast`. */
  id: string;
  tiles: DashboardTile[];
  className?: string;
}) {
  const stored = useStoredDashboardLayout(id);
  // Waiting for the stored layout avoids a visible jump from the default arrangement to the user's. A failed
  // read falls back to the defaults.
  if (stored.isPending) return null;
  return (
    <DashboardGrid
      // Keyed so another dashboard starts from its own stored layout.
      key={id}
      id={id}
      tiles={tiles}
      initialLayout={stored.data?.tiles ?? []}
      className={className}
    />
  );
}

function DashboardGrid({
  id,
  tiles,
  initialLayout,
  className,
}: {
  id: string;
  tiles: DashboardTile[];
  initialLayout: DashboardTileLayout[];
  className?: string;
}) {
  const t = useTranslations("dashboard");
  const [editing, setEditing] = useState(false);
  // The user's layout as stored (empty: the defaults), resolved against the declared tiles on every render.
  const [layout, setLayout] = useState<DashboardTileLayout[]>(initialLayout);
  useDashboardLayoutPersistence(id, { tiles: layout });

  const resolved = useMemo(() => resolveTiles(tiles, layout), [tiles, layout]);
  const byId = new Map(tiles.map((tile) => [tile.id, tile]));
  const visible = resolved.filter((tile) => !tile.hidden);

  /** Stores the edited tiles, or nothing (the defaults) when the edit lands on them again. */
  function apply(next: ResolvedTile[]) {
    setLayout(isDefaultLayout(tiles, next) ? [] : next);
  }

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (!over || active.id === over.id) return;
    apply(moveTile(resolved, String(active.id), String(over.id)));
  }

  return (
    <div className={cn("space-y-3", className)}>
      <DashboardToolbar
        editing={editing}
        onEditingChange={setEditing}
        hidden={resolved
          .filter((tile) => tile.hidden)
          .map((tile) => ({ id: tile.id, title: byId.get(tile.id)?.title }))}
        onShow={(tileId) =>
          apply(updateTile(resolved, tileId, { hidden: false }))
        }
        canReset={layout.length > 0}
        onReset={() => setLayout([])}
      />
      {visible.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t("noTiles")}</p>
      ) : (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext
            items={visible.map((tile) => tile.id)}
            strategy={rectSortingStrategy}
          >
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
              {visible.map((tile) => (
                <DashboardTileCard
                  key={tile.id}
                  tile={tile}
                  title={byId.get(tile.id)?.title}
                  fixedHeight={byId.get(tile.id)?.fixedHeight}
                  editing={editing}
                  onChange={(patch) =>
                    apply(updateTile(resolved, tile.id, patch))
                  }
                >
                  {byId.get(tile.id)?.render(TILE_HEIGHT_CLASS[tile.height])}
                </DashboardTileCard>
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}
    </div>
  );
}
