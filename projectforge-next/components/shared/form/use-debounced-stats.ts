"use client";

import { useEffect, useMemo, useState } from "react";
import { useStore } from "@tanstack/react-form";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useEntityData, useEntityEditForm } from "./form-context";

/**
 * What the server computes for the entity form while it is being edited (e.g. `/rs/vacation/stats`): the answer
 * for the slice of the form's values it depends on ([input]). As long as the form holds what was loaded, the loaded
 * entry stands in: the backend computed it with it (`transformFromDB`, `newBaseDTO`).
 *
 * Debounced, because a date is typed rather than picked as often as not. Nothing is asked before the first delay has
 * passed: the fields render while the form still holds its blank defaults, which it is reset from onto the loaded
 * entry only afterwards — a request for that blank state would answer empty values.
 *
 * @param entity Keys the queries (`[entity, "stats", …]`).
 * @param input The values the answer depends on, from the form's values.
 * @param toFormValues The form values of the loaded entry, to tell whether the form still holds it.
 * @param fetch Asks the server for the given input.
 * @param unknown Merged over the loaded entry if the request failed: blank rather than the stored entry's (or the
 * previous answer's) values, which would read as current.
 */
export function useDebouncedStats<TData extends object, TValues, TStats>({
  entity,
  input,
  toFormValues,
  fetch,
  unknown,
}: {
  entity: string;
  input: (values: TValues) => unknown;
  toFormValues: (data: TData) => TValues;
  fetch: (input: unknown, signal?: AbortSignal) => Promise<TStats>;
  unknown: Partial<TStats>;
}): TStats | TData | undefined {
  const form = useEntityEditForm();
  const data = useEntityData<TData>();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => JSON.stringify(input(state.values as TValues))
  ) as string;
  // The serialised slice is what is debounced and what keys the query: the selector builds a new object on every
  // render, so an object identity would never settle and the debounce would never fire.
  const [key, setKey] = useState<string | null>(null);
  useEffect(() => {
    const timer = setTimeout(() => setKey(values), 300);
    return () => clearTimeout(timer);
  }, [values]);
  // `input` and `toFormValues` are module functions of the caller: stable, not dependencies.
  const loadedKey = useMemo(
    () => (data ? JSON.stringify(input(toFormValues(data))) : null),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [data]
  );
  const asLoaded = key == null || key === loadedKey;
  const query = useQuery({
    queryKey: [entity, "stats", key],
    queryFn: ({ signal }) => fetch(JSON.parse(key!), signal),
    enabled: !asLoaded,
    placeholderData: keepPreviousData,
    // The answer is a pure function of what was sent (and of the stored entries, which a save refreshes).
    staleTime: 30_000,
    // Not asked again: until the global retries gave up (seconds), the previous answer would stand as current.
    retry: false,
  });
  if (asLoaded) return data;
  if (query.isError) return { ...data, ...unknown } as TStats;
  return query.data ?? data;
}
