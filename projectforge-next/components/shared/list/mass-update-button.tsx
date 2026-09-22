"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { HugeiconsIcon } from "@hugeicons/react";
import { CheckListIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import type { MassUpdateDef } from "@/lib/page-def/types";

/**
 * Takes the picked rows to the mass update page — the counterpart of Wicket's "multi selection".
 *
 * The ids are not passed: they live in the HTTP session, registered and narrowed there by the
 * selection mode (`startSelection` / `select`, see useListSelection), which is why the page can be
 * routed to with no parameters at all. What this does have to do is flush a *pending* `select`: the
 * ticks are posted debounced, and the page reads them the moment it mounts.
 */
export function MassUpdateButton({
  massUpdate,
  selectedIds,
  flush,
}: {
  massUpdate: MassUpdateDef;
  selectedIds: number[];
  /** Posts the ticks the mode still holds back, see ListSelection.flush. */
  flush: () => Promise<void>;
}) {
  const t = useTranslations();
  const router = useRouter();

  const start = useMutation({
    mutationFn: flush,
    onSuccess: () => router.push(massUpdate.route),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const disabled = selectedIds.length === 0 || start.isPending;
  return (
    <Button
      type="button"
      variant="default"
      size="sm"
      className="h-6 gap-1.5 px-2"
      disabled={disabled}
      onClick={() => start.mutate()}
    >
      {start.isPending ? (
        <Spinner className="h-3.5 w-3.5 border-2" />
      ) : (
        <HugeiconsIcon icon={CheckListIcon} size={14} aria-hidden />
      )}
      {/* Named after where it leads, not after the mode that fills it: the toggle in the toolbar is
          "Mehrfachauswahl", and a second button of that name says nothing about what pressing it
          does. Its own key rather than the bare `massUpdate`, whose leaf a scan cannot find (see
          NextI18nKeyScanner: a candidate needs a dot). */}
      {t("massUpdate.button")}
    </Button>
  );
}
