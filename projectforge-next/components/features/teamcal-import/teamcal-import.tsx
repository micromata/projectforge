"use client";

import { useMemo, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ImportFeature } from "@/components/shared/import/import-feature";
import type {
  ImportConfig,
  ImportView,
} from "@/components/shared/import/import-types";
import { importStateQueryKey } from "@/components/shared/import/use-import";
import { ENTITY, setTeamCalImportTarget } from "@/lib/rs/teamcal-import";
import { toast } from "@/lib/toast";
import { CalendarTargetSelect } from "./calendar-target-select";
import { TEAMCAL_IMPORT_COLUMNS } from "./columns";

interface Props {
  /** The calendar preselected by the caller (`?teamCalId=`), e.g. the calendar page's default calendar. */
  initialTeamCalId: number | null;
}

/**
 * The ICS import of team events, a consumer of the generic {@link ImportFeature}. What it adds is the target
 * calendar: chosen before the drop, it is sent with the upload; changed afterwards, the stash is reconciled
 * against the new calendar (`target`). The user stays on the page after the commit, since there is no list
 * of events to return to.
 */
export function TeamCalImport({ initialTeamCalId }: Props) {
  const queryClient = useQueryClient();
  const [teamCalId, setTeamCalId] = useState<number | null>(initialTeamCalId);

  const target = useMutation({
    mutationFn: (id: number) => setTeamCalImportTarget(id),
    onSuccess: (view, id) => {
      setTeamCalId(id);
      queryClient.setQueryData(importStateQueryKey(ENTITY), view);
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const config = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: ENTITY },
      titleKey: "plugins.teamcal.import.ics.tooltip",
      columns: TEAMCAL_IMPORT_COLUMNS,
      fileAccept: ".ics",
      returnRoute: "/calendar",
      stayAfterCommit: true,
      uploadFields:
        teamCalId != null ? { teamCalId: String(teamCalId) } : undefined,
      renderBeforeDrop: () => (
        <CalendarTargetSelect value={teamCalId} onChange={setTeamCalId} />
      ),
      renderAboveTable: (view: ImportView) => (
        <CalendarTargetSelect
          value={metaTeamCalId(view)}
          onChange={(id) => target.mutate(id)}
          disabled={target.isPending}
        />
      ),
    }),
    [teamCalId, target]
  );

  return <ImportFeature config={config} />;
}

/** The calendar the stash is reconciled against (`extraViewMeta`), null while none is chosen. */
function metaTeamCalId(view: ImportView): number | null {
  const id = view.meta?.teamCalId;
  return typeof id === "number" ? id : null;
}
