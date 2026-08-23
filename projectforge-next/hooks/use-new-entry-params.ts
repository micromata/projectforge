"use client";

import { useSearchParams } from "next/navigation";
import type { NewEntryParams } from "@/hooks/use-entity-detail";

/**
 * The declared parameters of an add url, for the preset the backend answers with.
 *
 * `/task/new?parentTaskId=42` means "a new task below task 42", and only the backend can turn that into
 * a preset — `TaskPagesRest.newBaseDO` resolves the parent, and with it the project the cost unit block
 * needs. So the parameter is not read into the form here; it is passed on to `{entity}/newEntry` and the
 * answer is what fills the form.
 *
 * Only the names the page declares are read (see EditDef.newEntryParams): the url of an edit page also
 * carries `returnTo`, which is the page's own business and must not reach the backend. A declared
 * parameter that is absent is simply left out.
 *
 * Like every `useSearchParams` reader this needs a `<Suspense>` boundary under the static export — the
 * routes that use it already have one for `useEditReturn`.
 */
export function useNewEntryParams(
  names?: readonly string[]
): NewEntryParams | undefined {
  const search = useSearchParams();
  if (!names?.length) return undefined;
  const params: NewEntryParams = {};
  for (const name of names) {
    const value = search.get(name);
    if (value !== null) params[name] = value;
  }
  return params;
}
