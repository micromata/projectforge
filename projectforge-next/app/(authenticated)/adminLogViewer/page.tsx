"use client";

import { useSearchParams } from "next/navigation";
import { LogFileSearch } from "@/components/features/log-viewer/log-file-search";
import { LogViewer } from "@/components/features/log-viewer/log-viewer";

/**
 * The admin log viewer (`/next/adminLogViewer`, MenuItemDefId.ADMIN_LOG_VIEWER): the last log events of the
 * whole system. Admin group only, enforced by AdminLogViewerRest (and 2FA-gated as ADMIN).
 *
 * With `?problem=<id>` (linked from the problem dashboard) the occurrences of that problem in the log files
 * instead, also of the days before the server's start.
 */
export default function AdminLogViewerRoute() {
  const problemId = Number(useSearchParams().get("problem"));
  return problemId > 0 ? (
    <LogFileSearch problemId={problemId} />
  ) : (
    <LogViewer admin id={null} />
  );
}
