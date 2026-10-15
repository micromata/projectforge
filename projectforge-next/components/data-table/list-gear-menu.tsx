"use client";

import { type ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Settings02Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DescribedMenuItem } from "@/components/shared/described-menu-item";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { LegacyMenuItem } from "@/components/shared/legacy-page-link";
import { useAuth } from "@/hooks/use-auth";
import { useFormatContext } from "@/hooks/use-format";
import { useListMeta } from "@/hooks/use-list-meta";
import { useReindex } from "@/hooks/use-reindex";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

export interface ListGearMenuProps {
  /** Backend entity, e.g. "book" — maps to /rs/{entity}/reindexNewest and friends. */
  entity: string;
  /**
   * The way back to the legacy list page, offered as the top entry when this entity has demoted it
   * from the prominent button into the menu (`ListMetaData.legacyListInMenu`, see LegacyMenuItem).
   * Absent while the entity still shows the button, or once it has no legacy counterpart at all.
   */
  legacyUrl?: string;
  /**
   * Additional entries of a specific list page, appended below the standard ones — as
   * [GearMenuItem]s, so they carry their explanation like the standard ones do.
   */
  children?: ReactNode;
  className?: string;
}

/**
 * Maintenance menu of a list page: re-index the search index — in full for a small entity, else the
 * newest entries (and in full for admins).
 *
 * The entries are the ones the backend put into the gear menu of the legacy list pages
 * (AbstractPagesRest.createListLayout), but declared here instead of read from `UILayout.pageMenu`:
 * they are the same for every entity, and this app builds its list pages itself. A page with extra
 * actions passes them as children.
 */
export function ListGearMenu({
  entity,
  legacyUrl,
  children,
  className,
}: ListGearMenuProps) {
  const t = useTranslations();
  const tMenu = useTranslations("menu");
  const { isAdmin } = useAuth();
  const reindex = useReindex(entity);
  // A cache read on a list page, which loads `listMeta` for its filter anyway. Until it is there, the
  // menu offers what a large entity offers — the safe side, since the endpoint checks the admin itself.
  const listMeta = useListMeta(entity).data;
  const fullReindexOnly = listMeta?.fullReindexOnly === true;
  const format = useFormatContext();

  return (
    <DropdownMenu>
      <HintTooltip text={t("settings")}>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="sm"
            aria-label={t("settings")}
            className={cn("gap-1", className)}
          >
            <HugeiconsIcon icon={Settings02Icon} size={16} />
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
          </Button>
        </DropdownMenuTrigger>
      </HintTooltip>
      <DropdownMenuContent align="end" className="w-72">
        {/* The bare menu titles live under "_": their own tooltip subkeys make them a namespace
            in the generated catalog (see GenerateNextI18nMessagesMain.JsonNode).

            The explanation stands in the entry instead of in a tooltip: a tooltip inside a dropdown
            competes with the menu for hover and focus, and these entries do something that is worth
            reading about *before* clicking — one of them re-indexes the whole database. */}
        {/* A small entity is rebuilt as a whole within seconds, so that is its only re-index, offered
            to everybody (see ListMetaData.fullReindexOnly). */}
        {fullReindexOnly && (
          <GearMenuItem
            label={tMenu("reindexAllDatabaseEntries._")}
            description={tMenu(
              "reindexAllDatabaseEntries.tooltip.contentSmall"
            )}
            onSelect={() => void reindex.start(true)}
          />
        )}
        {!fullReindexOnly && (
          <GearMenuItem
            label={tMenu("reindexNewestDatabaseEntries._")}
            description={tMenu("reindexNewestDatabaseEntries.tooltip.content")}
            onSelect={() => void reindex.start(false)}
          />
        )}
        {/* Rebuilding a large entity as a whole hits the whole system, so it is for admins only — the
            endpoint checks that as well, this merely hides a dead entry. Shown once listMeta is
            there, which names the limit its explanation states. */}
        {listMeta && !fullReindexOnly && isAdmin && (
          <GearMenuItem
            label={tMenu("reindexAllDatabaseEntries._")}
            description={tMenu("reindexAllDatabaseEntries.tooltip.content", {
              arg0: formatNumber(listMeta.fullReindexMaxEntries, format, 0),
            })}
            onSelect={() => void reindex.start(true)}
          />
        )}
        {/* No separator of their own: they are maintenance entries like the ones above, and a child
            may well render nothing for this user (see OrderGearMenuActions). */}
        {children}
        {/* Last and parted from the maintenance entries: it leaves the page rather than acting on it,
            and once every page is trusted it is the entry that goes away with the legacy app. */}
        {legacyUrl && (
          <>
            <DropdownMenuSeparator />
            <LegacyMenuItem url={legacyUrl} />
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/**
 * One standard entry: what it does, and below it what that means. A [DescribedMenuItem] that always has
 * its explanation.
 */
export function GearMenuItem(props: {
  label: string;
  description: string;
  disabled?: boolean;
  onSelect: () => void;
}) {
  return <DescribedMenuItem {...props} />;
}
