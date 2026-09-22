"use client";

import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { getByPath } from "@/lib/dynamic/path";

/**
 * An INPUT whose value is another entity — a user, a group, a cost unit — picked by searching for it.
 *
 * The layout says which kind through its `dataType` and usually names no url: the endpoint follows
 * from the type (`user/autosearch`, `group/autosearch`, …), which is what the legacy renderer does as
 * well (ObjectSelect.jsx). An explicit `autoCompletionUrl` wins, for a page that narrows the search.
 *
 * The picked entry is written as the backend sent it (`{ id, displayName }`, an
 * AbstractPagesRest.DisplayObject) because that is what it expects back on save — the property is a
 * reference, not a text.
 */
export function DynamicEntityInput({ node }: DynamicComponentProps) {
  const { data, setData } = useDynamicLayout();

  const id = node.id as string;
  const dataType = (node.dataType as string).toLowerCase();
  const url =
    (node.autoCompletionUrl as string | undefined) ??
    `${dataType}/autosearch?search=:search`;
  const urlParams = node.autoCompletionUrlParams as
    | Record<string, string>
    | undefined;

  // The endpoint may want other fields of the form as context (`cost2/autosearch` and the project).
  const params = Object.fromEntries(
    Object.entries(urlParams ?? {}).map(([param, path]) => [
      param,
      getByPath(data, path),
    ])
  );

  const raw = getByPath(data, id) as EntityRef | null | undefined;
  const value = raw && typeof raw.id === "number" ? raw : null;

  return (
    <DynamicField node={node}>
      {(domId) => (
        <EntityAutocomplete
          id={domId}
          url={url}
          params={urlParams ? params : undefined}
          value={value}
          // Null clears the property, which is how the backend reads a missing reference.
          onChange={(entry) => setData({ [id]: entry })}
          autoFocus={node.focus as boolean | undefined}
        />
      )}
    </DynamicField>
  );
}
