"use client";

import { AdminLogViewerTab } from "@/components/features/log-viewer/admin-log-viewer-tab";
import { SystemDashboard } from "@/components/features/system-dashboard/system-dashboard";

/**
 * The system dashboard (`/next/systemDashboard`), formerly the problem dashboard (`/next/problemDashboard`) and
 * the system statistics (`/next/systemStatistics`). The admin log viewer (formerly `/next/adminLogViewer`) is
 * one of its tabs, composed here: the dashboard doesn't import another feature.
 */
export default function SystemDashboardRoute() {
  return <SystemDashboard logViewerTab={<AdminLogViewerTab />} />;
}
