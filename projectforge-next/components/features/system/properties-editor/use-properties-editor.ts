"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useUnsavedChangesWarning } from "@/hooks/use-unsaved-changes-warning";
import {
  fetchPropertiesEditor,
  saveProperties,
} from "@/lib/rs/properties-editor";
import { toast } from "@/lib/toast";
import {
  allParams,
  buildChanges,
  filterGroups,
  isValidKey,
  withAddedParams,
  type PropertyEdit,
  type PropertyEdits,
  type PropertyFilter,
} from "./properties-editor-model";

const QUERY_KEY = ["propertiesEditor"];

/**
 * The state of the properties editor: the file as loaded, the not yet saved edits, the filter and the
 * save. After a conflict (the file was modified meanwhile) the file is reloaded and the edits are kept,
 * so they can be checked and saved again on top of the new version.
 */
export function usePropertiesEditor() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: QUERY_KEY,
    queryFn: ({ signal }) => fetchPropertiesEditor(signal),
    staleTime: 0,
  });
  const [edits, setEdits] = useState<PropertyEdits>({});
  const [addedKeys, setAddedKeys] = useState<string[]>([]);
  const [filter, setFilter] = useState<PropertyFilter>({
    query: "",
    onlyInFile: false,
    onlyModified: false,
  });

  const groups = useMemo(
    () => withAddedParams(query.data?.groups ?? [], addedKeys),
    [query.data, addedKeys]
  );
  const params = useMemo(() => allParams(groups), [groups]);
  const changes = useMemo(() => buildChanges(params, edits), [params, edits]);
  const visibleGroups = useMemo(
    () => filterGroups(groups, filter, edits),
    [groups, filter, edits]
  );
  useUnsavedChangesWarning(changes.length > 0);

  const mutation = useMutation({
    mutationFn: () => saveProperties(query.data?.fileVersion ?? "", changes),
    onSuccess: async (res) => {
      if (res.success) {
        toast.success(res.message);
        setEdits({});
        setAddedKeys([]);
      } else {
        toast.error(res.message);
      }
      if (res.success || res.conflict) {
        await queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      }
    },
    onError: (err) =>
      toast.error(err instanceof Error ? err.message : String(err)),
  });

  function setEdit(key: string, edit: PropertyEdit | undefined) {
    setEdits((prev) => {
      const next = { ...prev };
      if (edit) next[key] = edit;
      else delete next[key];
      return next;
    });
  }

  /** Adds a new key (group "Other"); returns an error text if it isn't valid or exists already. */
  function addKey(key: string, value: string): string | null {
    if (!isValidKey(key))
      return t("system.propertiesEditor.addParameter.invalid");
    if (params.some((param) => param.key === key))
      return t("system.propertiesEditor.addParameter.exists");
    setAddedKeys((prev) => [...prev, key]);
    setEdit(key, { kind: "set", value });
    return null;
  }

  return {
    query,
    edits,
    setEdit,
    addKey,
    filter,
    setFilter,
    visibleGroups,
    changes,
    save: () => mutation.mutate(),
    saving: mutation.isPending,
  };
}
