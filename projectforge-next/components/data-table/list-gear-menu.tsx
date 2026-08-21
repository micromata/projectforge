"use client";

import { useState, type ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Settings02Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useAuth } from "@/hooks/use-auth";
import { useReindex } from "@/hooks/use-reindex";
import { showResponseMessage } from "@/lib/dynamic/response-toast";
import { resetListFilter } from "@/lib/rs/list-actions";
import type { ResponseAction } from "@/lib/rs/types";
import { cn } from "@/lib/utils";

export interface ListGearMenuProps {
  /** Backend entity, e.g. "book" — maps to /rs/{entity}/reindexNewest and friends. */
  entity: string;
  /**
   * Clears the page's own filter state. The endpoint only drops what the server stores, so the
   * visible filter, search string, sorting and column layout have to be reset by the caller.
   */
  onFilterReset: () => void;
  /** Additional entries of a specific list page, appended below the standard ones. */
  children?: ReactNode;
  className?: string;
}

/**
 * Maintenance menu of a list page: re-index the search index and reset the filter.
 *
 * The entries are the ones the backend put into the gear menu of the legacy list pages
 * (AbstractPagesRest.createListLayout), but declared here instead of read from `UILayout.pageMenu`:
 * they are the same for every entity, and this app builds its list pages itself. A page with extra
 * actions passes them as children.
 */
export function ListGearMenu({
  entity,
  onFilterReset,
  children,
  className,
}: ListGearMenuProps) {
  const t = useTranslations();
  const tMenu = useTranslations("menu");
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();
  const reindex = useReindex(entity);
  // Only for the filter reset — the re-index runs are serialized by the backend's job queue.
  const [running, setRunning] = useState(false);

  /**
   * Runs an action and reports its outcome: the endpoint answers with a TOAST action whose text the
   * backend has already translated, so success needs no text of our own.
   */
  async function run(action: () => Promise<ResponseAction>): Promise<boolean> {
    setRunning(true);
    try {
      const response = await action();
      if (response.message) showResponseMessage(response.message);
      return true;
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
      return false;
    } finally {
      setRunning(false);
    }
  }

  async function resetFilter() {
    if (!(await run(() => resetListFilter(entity)))) return;
    onFilterReset();
    // The server dropped the stored filter and grid state with it, so the cached copies of both
    // would otherwise come back on the next mount.
    await queryClient.invalidateQueries({ queryKey: ["listMeta", entity] });
    await queryClient.invalidateQueries({
      queryKey: ["columnStates", `/rs/${entity}/columnStates`],
    });
  }

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
            competes with the menu for hover and focus, and these three entries do something that is
            worth reading about *before* clicking — one of them re-indexes the whole database. */}
        <GearMenuItem
          label={tMenu("reindexNewestDatabaseEntries._")}
          description={tMenu("reindexNewestDatabaseEntries.tooltip.content")}
          onSelect={() => void reindex.start(false)}
        />
        {/* Rebuilding everything includes the history and hits the whole system, so it is for admins
            only — the endpoint checks that as well, this merely hides a dead entry. */}
        {isAdmin && (
          <GearMenuItem
            label={tMenu("reindexAllDatabaseEntries._")}
            description={tMenu("reindexAllDatabaseEntries.tooltip.content")}
            onSelect={() => void reindex.start(true)}
          />
        )}
        <GearMenuItem
          label={tMenu("resetFilter._")}
          description={tMenu("resetFilter.info")}
          disabled={running}
          onSelect={() => void resetFilter()}
        />
        {children && (
          <>
            <DropdownMenuSeparator />
            {children}
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/** One standard entry: what it does, and below it what that means. */
function GearMenuItem({
  label,
  description,
  disabled,
  onSelect,
}: {
  label: string;
  description: string;
  disabled?: boolean;
  onSelect: () => void;
}) {
  return (
    <DropdownMenuItem
      disabled={disabled}
      onSelect={onSelect}
      className="flex-col items-start gap-0.5"
    >
      <span>{label}</span>
      {/* `whitespace-normal`: the menu primitive keeps its items on one line. */}
      <span className="text-[11px] whitespace-normal text-muted-foreground">
        {description}
      </span>
    </DropdownMenuItem>
  );
}
