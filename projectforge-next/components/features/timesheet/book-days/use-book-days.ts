"use client";

import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { postBookDays } from "@/lib/rs/timesheet-book-days";
import type { BookDaysRequest, BookDaysResult, BookDaysValues } from "./types";

const PLAN_KEY = ["timesheet", "bookDays", "plan"] as const;

export function toRequest(
  values: BookDaysValues,
  dryRun: boolean
): BookDaysRequest {
  return {
    userId: values.user?.id ?? null,
    taskId: values.taskId,
    kost2Id: values.kost2Id,
    location: values.location || null,
    description: values.description || null,
    startDate: values.startDate,
    endDate: values.endDate,
    startTime: values.startTime,
    hoursPerDay: values.hoursPerDay,
    halfDayBegin: values.halfDayBegin,
    halfDayEnd: values.halfDayEnd,
    dryRun,
  };
}

/**
 * The backend's dry run of the dialog as it stands, refreshed shortly after the user stops typing, and
 * the booking itself.
 *
 * The request is debounced as its JSON, not as the object: a fresh object every render would restart
 * the debounce forever. `current` tells whether the preview shown belongs to the values on screen —
 * only then may they be booked, so the button never books something the preview didn't show.
 */
export function useBookDays(
  values: BookDaysValues,
  onBooked: (result: BookDaysResult) => void,
  onError: (message: string) => void
) {
  const key = JSON.stringify(toRequest(values, true));
  const debouncedKey = useDebouncedValue(key, 300);
  const request = JSON.parse(debouncedKey) as BookDaysRequest;
  const preview = useQuery({
    queryKey: [...PLAN_KEY, debouncedKey],
    queryFn: ({ signal }) => postBookDays(request, signal),
    enabled: request.startDate != null && request.endDate != null,
    placeholderData: keepPreviousData,
    // A refusal (406) is an answer, not a hiccup: show it at once rather than after three retries.
    retry: false,
  });
  const booking = useMutation({
    mutationFn: () => postBookDays(toRequest(values, false)),
    onSuccess: onBooked,
    onError: (err) => onError(err instanceof Error ? err.message : String(err)),
  });
  const current =
    key === debouncedKey && !preview.isPlaceholderData && !preview.isFetching;
  return {
    days: preview.data?.days ?? [],
    bookCount: current && !preview.error ? (preview.data?.bookedCount ?? 0) : 0,
    loading: preview.isFetching || key !== debouncedKey,
    error: preview.error ? preview.error.message : null,
    current,
    booking,
  };
}
