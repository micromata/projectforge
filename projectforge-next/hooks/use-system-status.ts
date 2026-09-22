"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchSystemStatus } from "@/lib/rs/client";
import type { SystemStatus } from "@/lib/rs/types";

/**
 * The application's own facts (`/rsPublic/systemStatus`): version, build, the configured logo.
 *
 * Public — no session needed — which is what lets the login page show the customer's logo as well.
 * Nothing in the response changes while the tab is open, so it is fetched once and kept.
 */
export function useSystemStatus() {
  return useQuery<SystemStatus>({
    queryKey: ["systemStatus"],
    queryFn: ({ signal }) => fetchSystemStatus(signal),
    staleTime: Infinity,
    retry: false,
  });
}
