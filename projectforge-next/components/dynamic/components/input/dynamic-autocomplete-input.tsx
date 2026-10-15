"use client";

import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import { SuggestInput } from "@/components/shared/suggest-input";
import { getByPath } from "@/lib/dynamic/path";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";

/**
 * A free-text INPUT that suggests values the backend has already seen for this property
 * (`{category}/autocomplete?property=…`, see AbstractPagesRest.getAutoCompletionForProperty).
 *
 * The box itself is the shared [SuggestInput], which a hand-built form field renders too; what is here
 * is only the layout node's half — where the value lives and which url and context parameters it named.
 */
export function DynamicAutoCompleteInput({ node }: DynamicComponentProps) {
  const { data, setData } = useDynamicLayout();

  const id = node.id as string;
  const url = node.autoCompletionUrl as string;
  const urlParams = node.autoCompletionUrlParams as
    | Record<string, string>
    | undefined;
  const raw = getByPath(data, id);
  const value = raw != null ? String(raw) : "";

  // The backend may want other fields of the form as context (e.g. the selected task).
  const params = Object.fromEntries(
    Object.entries(urlParams ?? {}).map(([param, path]) => [
      param,
      getByPath(data, path),
    ])
  );

  return (
    <DynamicField node={node}>
      {(domId, hasError) => (
        <SuggestInput
          id={domId}
          value={value}
          onChange={(next) => setData({ [id]: next })}
          suggest={(search, signal) =>
            fetchAutoCompletion<string>(url, search, params, signal)
          }
          queryKey={[url, params]}
          invalid={hasError}
          required={node.required as boolean | undefined}
          maxLength={node.maxLength as number | undefined}
          autoFocus={node.focus as boolean | undefined}
        />
      )}
    </DynamicField>
  );
}
