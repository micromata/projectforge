"use client";

import { useStore } from "@tanstack/react-form";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { postKost2Preview, type Kost2Preview } from "@/lib/rs/task";
import type { TaskValues } from "../task-schema";

/**
 * What the preview depends on, and nothing else: the black/white list, whether it is a black one, and
 * the task it belongs to — the id for a stored task, the parent for one being added, which is the only
 * way to resolve its project (see `Kost2PreviewRequest`).
 */
function previewInput(id: number | null, values: TaskValues) {
  return {
    id,
    parentTaskId: values.parentTask?.id ?? null,
    kost2BlackWhiteList: values.kost2BlackWhiteList,
    kost2IsBlackList: values.kost2IsBlackList,
  };
}

export interface Kost2PreviewState {
  /** Undefined while the first answer is on its way. */
  preview: Kost2Preview | undefined;
  /** Appends a picked cost unit to the list and writes back what the server made of it. */
  addKost2: (kost2Id: number) => void;
  isLoading: boolean;
}

/**
 * The cost units a task's black/white list resolves to, as the server computes them while the list is
 * being typed — the `projektKostLabel` of the Wicket form and its tooltip.
 *
 * Deliberately not computed here: matching the entries against the project's active cost units is
 * `TaskTree.getKost2List`, and appending a picked one is `TaskHelper.addKost2`, which has a branch a
 * TypeScript copy would get wrong (see lib/rs/task.ts). So the form's values go to
 * `/rs/task/kost2Preview` and the resolved list comes back.
 *
 * Debounced, because this is a read of something the user is still typing: every intermediate list
 * would otherwise be a request of its own.
 */
export function useKost2Preview(id: number | null): Kost2PreviewState {
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => JSON.stringify(previewInput(id, state.values as TaskValues))
  ) as string;
  // The serialised slice is what is debounced and what keys the query: the selector builds a new object
  // on every render, so an object identity would never settle and the debounce would never fire.
  const key = useDebouncedValue(values, 400);

  const query = useQuery({
    queryKey: ["task", "kost2Preview", key],
    queryFn: ({ signal }) => postKost2Preview(JSON.parse(key), signal),
    // The answer is a pure function of what was sent, so it stays valid while the user edits elsewhere.
    staleTime: 60_000,
  });

  const add = useMutation({
    // The *current* values, not the debounced ones: a pick has to append to the list as it stands.
    mutationFn: (kost2Id: number) =>
      postKost2Preview({
        ...previewInput(id, form.state.values as TaskValues),
        addKost2Id: kost2Id,
      }),
    onSuccess: (result) =>
      form.setFieldValue(
        "kost2BlackWhiteList",
        result.kost2BlackWhiteList ?? null
      ),
  });

  return {
    // Only the query's answer, although the pick's answer carries a preview too: that one belongs to
    // the list of the moment it was sent, and keeping it would go on showing it after the next
    // keystroke. Writing the field is itself what asks again.
    preview: query.data,
    addKost2: add.mutate,
    isLoading: query.isFetching || add.isPending,
  };
}
