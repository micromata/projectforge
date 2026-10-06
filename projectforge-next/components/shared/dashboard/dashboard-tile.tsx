"use client";

import type { ReactNode } from "react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { HugeiconsIcon } from "@hugeicons/react";
import { DragDropIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type {
  DashboardTileHeight,
  DashboardTileWidth,
} from "@/lib/rs/ui-settings";
import { cn } from "@/lib/utils";
import type { ResolvedTile, TilePatch } from "./dashboard-layout";
import { DashboardTileMenu } from "./dashboard-tile-menu";

/** Column span per width — spelled out, so Tailwind finds the classes. Below `lg` every tile spans the row. */
const WIDTH_CLASS: Record<DashboardTileWidth, string> = {
  third: "lg:col-span-4",
  half: "lg:col-span-6",
  twoThirds: "lg:col-span-8",
  full: "lg:col-span-12",
};

/** Chart height per height step; M is the height the charts had before they moved into tiles. */
export const TILE_HEIGHT_CLASS: Record<DashboardTileHeight, string> = {
  S: "h-64 w-full",
  M: "h-[27rem] w-full",
  L: "h-[36rem] w-full",
};

/**
 * One tile of a ChartDashboard: a card spanning the tile's width. In edit mode it shows a drag handle (only
 * the handle starts a drag, so the chart's tooltips keep working) and the tile menu.
 */
export function DashboardTileCard({
  tile,
  title,
  fixedHeight,
  editing,
  onChange,
  children,
}: {
  tile: ResolvedTile;
  title: ReactNode;
  fixedHeight?: boolean;
  editing: boolean;
  onChange: (patch: TilePatch) => void;
  children: ReactNode;
}) {
  const t = useTranslations("dashboard");
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: tile.id, disabled: !editing });

  return (
    <Card
      ref={setNodeRef}
      style={{
        // Translate only: the tiles differ in size, and the scale dnd-kit adds to match them would distort
        // the chart while it moves.
        transform: CSS.Translate.toString(transform),
        transition: isDragging ? "none" : (transition ?? undefined),
      }}
      className={cn(
        "col-span-1 min-w-0 gap-2",
        WIDTH_CLASS[tile.width],
        editing && "ring-2 ring-primary/30",
        isDragging && "relative z-10 opacity-80 shadow-lg ring-primary"
      )}
    >
      <CardHeader className="flex items-center gap-2">
        {editing && (
          <button
            type="button"
            ref={setActivatorNodeRef}
            className="-ml-1 flex cursor-grab touch-none rounded-sm p-1 text-muted-foreground hover:bg-accent active:cursor-grabbing"
            aria-label={t("dragHandle")}
            {...attributes}
            {...listeners}
          >
            <HugeiconsIcon icon={DragDropIcon} size={14} />
          </button>
        )}
        <CardTitle className="flex-1 font-semibold">{title}</CardTitle>
        {editing && (
          <DashboardTileMenu
            tile={tile}
            fixedHeight={fixedHeight}
            onChange={onChange}
          />
        )}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  );
}
