"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { MoreVerticalIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import type {
  DashboardTileHeight,
  DashboardTileWidth,
} from "@/lib/rs/ui-settings";
import {
  TILE_HEIGHTS,
  TILE_WIDTHS,
  type ResolvedTile,
  type TilePatch,
} from "./dashboard-layout";

/** The ⋮ menu of a tile in edit mode: width, height and hiding. */
export function DashboardTileMenu({
  tile,
  fixedHeight,
  onChange,
}: {
  tile: ResolvedTile;
  /** Hides the height steps, for content of its own height. */
  fixedHeight?: boolean;
  onChange: (patch: TilePatch) => void;
}) {
  const t = useTranslations("dashboard");
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          aria-label={t("tileMenu")}
        >
          <HugeiconsIcon icon={MoreVerticalIcon} size={14} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-44">
        <DropdownMenuLabel>{t("width._")}</DropdownMenuLabel>
        <DropdownMenuRadioGroup
          value={tile.width}
          onValueChange={(value) =>
            onChange({ width: value as DashboardTileWidth })
          }
        >
          {TILE_WIDTHS.map((width) => (
            <DropdownMenuRadioItem key={width} value={width}>
              {/* The family is exported as a whole (PREFIXES of the i18n generator). */}
              {t(`width.${width}`)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
        {!fixedHeight && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuLabel>{t("height._")}</DropdownMenuLabel>
            <DropdownMenuRadioGroup
              value={tile.height}
              onValueChange={(value) =>
                onChange({ height: value as DashboardTileHeight })
              }
            >
              {TILE_HEIGHTS.map((height) => (
                <DropdownMenuRadioItem key={height} value={height}>
                  {t(`height.${height}`)}
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuRadioGroup>
          </>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => onChange({ hidden: true })}>
          {t("hide")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
