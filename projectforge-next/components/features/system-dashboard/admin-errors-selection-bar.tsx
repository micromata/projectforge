"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { SelectionBar } from "@/components/shared/list/selection-bar";
import { Button } from "@/components/ui/button";
import { useSelectAllShortcut } from "@/hooks/use-select-all-shortcut";
import {
  updateAdminErrors,
  type LogGroupAction,
  type LogGroupUpdate,
} from "@/lib/rs/admin-errors";
import { toast } from "@/lib/toast";

/**
 * The selection bar of the system dashboard's problems, with the status actions for all picked problems at once:
 * the ones of a problem's detail (see AdminErrorActions) except the notify override, which is a rule per event
 * rather than a status. Offered all of them, as the picked problems may be in any status; an action a problem
 * already has changes nothing there. Mounted only in selection mode, which is when `CTRL-A` picks all.
 */
export function AdminErrorsSelectionBar({
  ids,
  onSelectAll,
  onClear,
  onLeave,
}: {
  ids: number[];
  onSelectAll: () => void;
  /** Also after a change: the selection is spent, the changed problems may even have left the filter. */
  onClear: () => void;
  onLeave: () => void;
}) {
  useSelectAllShortcut(true, onSelectAll);
  const t = useTranslations();
  const queryClient = useQueryClient();
  const update = useMutation({
    mutationFn: (change: Omit<LogGroupUpdate, "ids">) =>
      updateAdminErrors({ ...change, ids }),
    onSuccess: (count) => {
      toast.success(
        t("system.admin.adminErrors.massUpdate.done", { arg0: count })
      );
      onClear();
      return queryClient.invalidateQueries({ queryKey: ["adminErrors"] });
    },
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });
  const button = (label: string, action: LogGroupAction, muteDays?: number) => (
    <Button
      key={`${action}-${muteDays ?? ""}`}
      type="button"
      variant="outline"
      size="sm"
      className="h-6 px-2"
      disabled={ids.length === 0 || update.isPending}
      onClick={() => update.mutate({ action, muteDays })}
    >
      {label}
    </Button>
  );
  return (
    // Across the whole width as in the lists: the tab's padding is undone.
    <div className="-mx-4 mb-2">
      <SelectionBar
        count={ids.length}
        onSelectAll={onSelectAll}
        onClear={onClear}
        onLeave={onLeave}
        actions={
          <>
            {button(
              t("system.admin.adminErrors.action.acknowledge"),
              "ACKNOWLEDGE"
            )}
            {button(t("system.admin.adminErrors.action.resolve"), "RESOLVE")}
            {button(t("system.admin.adminErrors.action.ignore"), "IGNORE")}
            {button(t("system.admin.adminErrors.action.reopen"), "REOPEN")}
            {button(t("system.admin.adminErrors.action.mute.day"), "MUTE", 1)}
            {[7, 30].map((days) =>
              button(
                t("system.admin.adminErrors.action.mute.days", { arg0: days }),
                "MUTE",
                days
              )
            )}
            {button(t("system.admin.adminErrors.action.unmute"), "UNMUTE")}
          </>
        }
      />
    </div>
  );
}
