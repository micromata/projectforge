"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { fetchGanttPreview } from "@/lib/rs/gantt";
import type { GanttValues } from "../gantt-schema";
import type { GanttDiagramDetail } from "../types";

interface FormState {
  values: GanttValues;
}

/**
 * The chart as the server draws it from the unsaved form, a moment after the last change. The tree table
 * and the preview both call this; one query key, so one request serves both. The open nodes are left out
 * of the key: browsing the tree changes nothing in the drawing.
 */
export function useGanttPreview() {
  const form = useEntityEditForm();
  const values = useStore(form.store, (s: unknown) => (s as FormState).values);
  const key = useDebouncedValue(
    JSON.stringify({ ...values, openNodes: null }),
    500
  );
  return useQuery({
    queryKey: ["ganttPreview", key],
    queryFn: ({ signal }) =>
      fetchGanttPreview(JSON.parse(key) as GanttDiagramDetail, signal),
    enabled: values.root != null,
    placeholderData: keepPreviousData,
  });
}
