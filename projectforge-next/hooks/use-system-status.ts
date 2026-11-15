"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchSystemStatus } from "@/lib/rs/client";
import type { SystemStatus } from "@/lib/rs/types";

/**
 * The application's own facts (`/rsPublic/systemStatus`): version, build, the configured logo.
 *
 * Public — no session needed — which is what lets the login page show the customer's logo as well.
 * It changes only if an admin saves another logo in the properties editor (which then invalidates
 * [SYSTEM_STATUS_QUERY_KEY]), so it is fetched once and kept.
 */
export const SYSTEM_STATUS_QUERY_KEY = ["systemStatus"];

export function useSystemStatus() {
  return useQuery<SystemStatus>({
    queryKey: SYSTEM_STATUS_QUERY_KEY,
    queryFn: ({ signal }) => fetchSystemStatus(signal),
    staleTime: Infinity,
    retry: false,
  });
}
