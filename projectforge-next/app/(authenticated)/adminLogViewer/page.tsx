"use client";

import { useSearchParams } from "next/navigation";
import { LogFileQuery } from "@/components/features/log-viewer/log-file-query";
import { LogFileSearch } from "@/components/features/log-viewer/log-file-search";
import { LogSourceSwitch } from "@/components/features/log-viewer/log-source-switch";
import { LogViewer } from "@/components/features/log-viewer/log-viewer";

/**
 * The admin log viewer (`/next/adminLogViewer`, MenuItemDefId.ADMIN_LOG_VIEWER): the last log events of the
 * whole system. Admin group only, enforced by AdminLogViewerRest (and 2FA-gated as ADMIN).
 *
 * With `?source=files` the search over the log files instead (text, period, level, logger, with the context
 * of a hit). With `?incident=<id>` (linked from the problem dashboard) the occurrences of that problem in the
 * log files, also of the days before the server's start.
 */
export default function AdminLogViewerRoute() {
  const params = useSearchParams();
  const problemId = Number(params.get("incident"));
  if (problemId > 0) return <LogFileSearch problemId={problemId} />;
  if (params.get("source") === "files") return <LogFileQuery />;
  return (
    <LogViewer
      admin
      id={null}
      titleCenter={<LogSourceSwitch value="memory" />}
    />
  );
}
