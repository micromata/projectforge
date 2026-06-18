"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchHistory } from "@/lib/rs/client";
import type { HistoryEntry } from "../types";

export function useBookHistory(id: number) {
  return useQuery<HistoryEntry[]>({
    queryKey: ["book", "history", id],
    queryFn: ({ signal }) => fetchHistory<HistoryEntry>("book", id, signal),
    enabled: Number.isFinite(id) && id > 0,
  });
}
