/**
 * The system statistics page (`org.projectforge.rest.SystemStatisticsRest`). The sections are fetched
 * one by one (in parallel), so the fast ones are shown without waiting for the slow ones (gc, disk usage).
 * Non-admins only get the sections and entries they may see.
 */

import { request } from "./client";
import type {
  SystemStatisticsSection,
  SystemStatisticsSectionData,
} from "@/components/features/system-dashboard/system-statistics-types";

export function fetchSystemStatisticsSections(
  signal?: AbortSignal
): Promise<SystemStatisticsSection[]> {
  return request<SystemStatisticsSection[]>(
    "/rs/systemStatistics/sections",
    { method: "GET" },
    signal
  );
}

export function fetchSystemStatisticsSection(
  id: string,
  signal?: AbortSignal
): Promise<SystemStatisticsSectionData> {
  return request<SystemStatisticsSectionData>(
    `/rs/systemStatistics/section/${encodeURIComponent(id)}`,
    { method: "GET" },
    signal
  );
}
