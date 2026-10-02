"use client";

import { LogViewer } from "@/components/features/log-viewer/log-viewer";

/**
 * The admin log viewer (`/next/adminLogViewer`, MenuItemDefId.ADMIN_LOG_VIEWER): the last log events of the
 * whole system. Admin group only, enforced by AdminLogViewerRest (and 2FA-gated as ADMIN).
 */
export default function AdminLogViewerRoute() {
  return <LogViewer admin id={null} />;
}
