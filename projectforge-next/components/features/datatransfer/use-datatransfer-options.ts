"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchDataTransferOptions } from "@/lib/rs/datatransfer";

/**
 * The choices of the admin form and the gateway flag (`DataTransferAreaEntityRest.getOptions`). Static
 * per installation, so read once and kept.
 */
export function useDataTransferOptions() {
  return useQuery({
    queryKey: ["datatransfer", "options"],
    queryFn: ({ signal }) => fetchDataTransferOptions(signal),
    staleTime: 60 * 60_000,
    refetchOnWindowFocus: false,
  }).data;
}
