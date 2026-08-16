"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { HugeiconsIcon } from "@hugeicons/react";
import { CheckListIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { selectEntries, startMultiSelection } from "@/lib/rs/multi-select";
import type { MassUpdateDef } from "@/lib/page-def/types";
import type { MagicFilter } from "@/lib/rs/types";

/**
 * Takes the picked rows to the mass update page — the counterpart of Wicket's "multi selection".
 *
 * Two calls before the route, both of them the backend's protocol and neither of them skippable: the
 * selection lives in the HTTP session and is opened per list (`startSelection` registers everything the
 * filter matched), and only then can the ticked subset be narrowed into it (`select`). The ids never
 * travel in the url, which is why the page can be routed to with no parameters at all.
 */
export function MassUpdateButton({
  entity,
  massUpdate,
  filter,
  selectedIds,
}: {
  entity: string;
  massUpdate: MassUpdateDef;
  /** The filter the list is showing, i.e. exactly the entries that may be picked from. */
  filter: MagicFilter;
  selectedIds: number[];
}) {
  const t = useTranslations();
  const router = useRouter();

  const start = useMutation({
    mutationFn: async () => {
      await startMultiSelection(entity, filter);
      await selectEntries(massUpdate.endpoint, selectedIds);
    },
    onSuccess: () => router.push(massUpdate.route),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const disabled = selectedIds.length === 0 || start.isPending;
  return (
    <HintTooltip
      title={t("multiselection.aggrid.selection.info.title")}
      // How to pick rows, as markdown from the bundle — the gestures it lists are the ones
      // `use-row-selection` implements. On the button rather than in a box above the table: it is a
      // footnote of the feature, and a permanent box would cost a table row of height on every visit
      // to say what a reader needs once. The key still names ag-grid, the legacy grid the text was
      // written for; the text is the right one and duplicating it under a nicer key would mean two
      // translations to keep in step.
      text={`${t("multiselection.aggrid.selection.info.message")}\n\n${t(
        "massUpdate.entriesFound",
        { arg0: selectedIds.length }
      )}`}
    >
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        disabled={disabled}
        onClick={() => start.mutate()}
      >
        {start.isPending ? (
          <Spinner className="h-3.5 w-3.5 border-2" />
        ) : (
          <HugeiconsIcon icon={CheckListIcon} size={14} aria-hidden />
        )}
        {/* The count is part of the label rather than a badge beside it: while nothing is picked the
            button is disabled, and "0" is then the reason. */}
        {`${t("multiselection.button")} (${selectedIds.length})`}
      </Button>
    </HintTooltip>
  );
}
