"use client";

import { useSearchParams } from "next/navigation";
import { TabsContent } from "@/components/ui/tabs";
import { LogFileQuery } from "./log-file-query";
import { LogFileSearch } from "./log-file-search";
import { LogViewerEmbedded } from "./log-viewer-frame";
import {
  INCIDENT_PARAM,
  LogSourceSwitch,
  SOURCE_PARAM,
} from "./log-source-switch";
import { LogViewer } from "./log-viewer";

/** The id of the system dashboard's tab of the admin log viewer (`?tab=logs`). */
export const ADMIN_LOG_VIEWER_TAB = "logs";

/**
 * The admin log viewer as tab of the system dashboard (`/next/systemDashboard?tab=logs`, admin group only,
 * enforced by AdminLogViewerRest and 2FA-gated as ADMIN): the last log events of the whole system in memory,
 * with `?source=files` the search over the log files (text, period, level, logger, with the context of a hit),
 * with `?incident=<id>` (linked from a problem's detail) the occurrences of that problem in the log files. The
 * former page `/next/adminLogViewer` is redirected here, its query carried over (OrphanedLinkFilter).
 */
export function AdminLogViewerTab() {
  return (
    <TabsContent
      value={ADMIN_LOG_VIEWER_TAB}
      className="flex min-h-0 flex-col text-sm"
    >
      <LogViewerEmbedded>
        <AdminLogViewerView />
      </LogViewerEmbedded>
    </TabsContent>
  );
}

/** Mounted with the open tab only, so nothing is fetched before it is shown. */
function AdminLogViewerView() {
  const params = useSearchParams();
  const problemId = Number(params.get(INCIDENT_PARAM));
  if (problemId > 0)
    return <LogFileSearch key={problemId} problemId={problemId} />;
  if (params.get(SOURCE_PARAM) === "files") return <LogFileQuery />;
  return (
    <LogViewer
      admin
      id={null}
      titleCenter={<LogSourceSwitch value="memory" />}
    />
  );
}
