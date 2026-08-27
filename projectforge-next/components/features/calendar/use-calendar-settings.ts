"use client";

import { useCallback } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { fetchCalendarSettings, saveCalendarSettings } from "@/lib/rs/calendar";
import type { CalendarSettings } from "@/lib/rs/calendar-types";
import { CALENDAR_EVENTS_KEY } from "./use-calendar-init";

const CALENDAR_SETTINGS_KEY = ["calendar", "settings"] as const;

/**
 * The colour settings shown in the gear dialog, persisted apart from the filter (see
 * {@link CalendarSettings}). The settings are only fetched when this hook mounts, which the dialog gates
 * on being open. `change` merges a partial over the current settings and persists the whole object — the
 * backend's only write is a bulk persist — then invalidates the events, because the colours are baked
 * into the server-rendered events (timesheet/vacation colours) and must be repainted.
 */
export function useCalendarSettings() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: CALENDAR_SETTINGS_KEY,
    queryFn: ({ signal }) => fetchCalendarSettings(signal),
    staleTime: 30_000,
  });

  const mutation = useMutation({
    mutationFn: (settings: CalendarSettings) => saveCalendarSettings(settings),
    onSuccess: async (saved) => {
      queryClient.setQueryData<CalendarSettings>(CALENDAR_SETTINGS_KEY, saved);
      await queryClient.invalidateQueries({ queryKey: CALENDAR_EVENTS_KEY });
    },
    onError: (err) =>
      toast.error(
        err instanceof Error ? err.message : "Saving calendar settings failed."
      ),
  });

  const change = useCallback(
    (partial: Partial<CalendarSettings>) => {
      const current = query.data;
      if (!current) return;
      mutation.mutate({ ...current, ...partial });
    },
    [mutation, query.data]
  );

  return { settings: query.data, isLoading: query.isLoading, change };
}
