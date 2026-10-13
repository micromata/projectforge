"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useAuth } from "@/hooks/use-auth";
import { useExportDownload } from "@/hooks/use-export-download";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import { updateSearchParams } from "@/lib/search-params";
import {
  downloadAdminErrors,
  fetchAdminErrors,
  fetchAdminSubsystems,
  type LogGroupFilter,
  type LogGroupScope,
} from "@/lib/rs/admin-errors";

const START_FILTER: LogGroupFilter = { status: "OPEN", days: 7 };

/** The subsystem of a tile, see AdminSubsystemTiles. */
export const SUBSYSTEM_PARAM = "subsystem";

/** The scope of a key figure, see AdminErrorsSummary. */
export const SCOPE_PARAM = "scope";

const SCOPES: readonly LogGroupScope[] = ["NEW_24H", "REGRESSION"];

/**
 * The state of the problem tabs of the system dashboard (overview and problems, admin group only): the problems
 * the log aggregation counted, the filter and search over them and the problem whose detail is open. The error
 * digest links a problem as `?id=<id>`, which opens its detail at once ([linkedId]).
 */
export function useProblemDashboard() {
  const { isAdmin, isLoading } = useAuth();
  const params = useSearchParams();
  const linkedId = Number(params.get("id")) || null;
  const [detailId, setDetailId] = useState<number | null>(linkedId);
  // Subsystem and scope are kept in the url, so that the back button returns to the previous view.
  const subsystem = params.get(SUBSYSTEM_PARAM) || null;
  const scope = SCOPES.find((it) => it === params.get(SCOPE_PARAM)) ?? null;
  // Status, category and period stay local: going back shouldn't undo every change of a select. All of them are
  // the server's; the search works on the loaded problems (see AdminErrorsTable), so typing doesn't fetch the list
  // anew on every key.
  const [localFilter, setFilter] = useState<LogGroupFilter>(START_FILTER);
  const filter: LogGroupFilter = { ...localFilter, subsystem, scope };
  const [search, setSearch] = useState("");
  const closeDetail = () => {
    setDetailId(null);
    // Drops the link's id, so that a reload doesn't open the detail again.
    if (params.has("id")) updateSearchParams({ id: null }, "replace");
  };

  const list = useQuery({
    queryKey: ["adminErrors", "list", filter],
    queryFn: ({ signal }) => fetchAdminErrors(filter, signal),
    enabled: isAdmin,
    placeholderData: keepPreviousData,
  });
  const subsystems = useQuery({
    queryKey: ["adminErrors", "subsystems"],
    queryFn: ({ signal }) => fetchAdminSubsystems(signal),
    enabled: isAdmin,
  });
  const subsystemTitle = subsystem
    ? (subsystems.data?.find((it) => it.id === subsystem)?.title ?? subsystem)
    : null;
  // All problems of the filter and the search, not only the listed ones, as JSON for an analysis (e.g. by an AI).
  const download = useExportDownload(() =>
    downloadAdminErrors({ ...filter, search })
  );
  const denied = (!isLoading && !isAdmin) || isAccessDenied(list.error);

  return {
    linkedId,
    detailId,
    setDetailId,
    closeDetail,
    filter,
    setFilter,
    search,
    setSearch,
    list,
    subsystems,
    subsystemTitle,
    download,
    denied,
  };
}
