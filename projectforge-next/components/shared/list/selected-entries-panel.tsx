"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { cn } from "@/lib/utils";
import { fetchSelectedEntries } from "@/lib/rs/multi-select";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { ColumnDeclaration } from "@/lib/page-def/types";
import { SelectedEntriesTable } from "./selected-entries-table";

/**
 * "Which entries did I pick?", answered on both pages that ask it: above the mass update's fields, and
 * in the list's selection bar.
 *
 * Closed by default and fetched only when opened — the answer is a whole result set, and most visits to
 * the mass update page do not need to see it.
 *
 * The rows come from the server (`{page}/selectedList`), not from the list's own: the ticks are kept
 * across a change of the filter, so the selection is the union over several filter runs while the list
 * only holds what the current filter matched. That case is exactly the one this panel exists for.
 */
export function SelectedEntriesPanel<
  Row extends ListRow,
  M extends EntityMetadata,
>({
  endpoint,
  metadata,
  columns,
  selectionKey,
  count,
  beforeFetch,
  className,
}: {
  /** REST base of the mass update page (`invoiceSelected`) — where the session keeps the selection. */
  endpoint: string;
  metadata: M;
  /** The list's own column declarations, so a row reads as it does there. */
  columns: ColumnDeclaration<Row, M>[];
  /** What a changed selection reads as, so the query refetches when the ticks changed. */
  selectionKey: string;
  /** How many are picked, for the label — known without opening the panel. */
  count: number;
  /** Posts ticks that are still held back, awaited before the fetch (see ListSelection.flush). */
  beforeFetch?: () => Promise<void>;
  className?: string;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  const entries = useQuery({
    queryKey: ["selectedEntries", endpoint, selectionKey],
    queryFn: async ({ signal }) => {
      await beforeFetch?.();
      return fetchSelectedEntries<Row>(endpoint, signal);
    },
    enabled: open,
    // Session state a second tab could have changed, as the page's metadata is.
    staleTime: 0,
  });

  return (
    <Collapsible
      open={open}
      onOpenChange={setOpen}
      className={cn("rounded-md border border-border bg-background", className)}
    >
      <CollapsibleTrigger className="flex w-full cursor-pointer items-center gap-2 px-3 py-1.5 text-left text-xs">
        <HugeiconsIcon
          icon={ArrowDown01Icon}
          size={14}
          aria-hidden
          className={cn(
            "shrink-0 text-muted-foreground transition-transform",
            !open && "-rotate-90"
          )}
        />
        <span className="font-semibold">{t("massUpdate.selectedEntries")}</span>
        <span className="text-muted-foreground">({count})</span>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="border-t border-border/60">
          {entries.isError ? (
            <Alert variant="destructive" className="rounded-none border-0">
              <AlertDescription>
                {entries.error instanceof Error
                  ? entries.error.message
                  : t("massUpdate.error.unspecifiedError")}
              </AlertDescription>
            </Alert>
          ) : (
            <SelectedEntriesTable<Row, M>
              metadata={metadata}
              columns={columns}
              rows={entries.data?.resultSet ?? []}
              isLoading={entries.isPending}
            />
          )}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}
