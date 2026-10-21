"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { CodeDiffButton } from "@/components/shared/code-editor/code-diff-button";
import { useHistoryVersions } from "@/components/shared/code-editor/use-history-versions";
import { prettyJson } from "@/lib/pretty-json";
import { customerGroupsToJson } from "@/lib/rs/customer-groups";
import { CONFIGURATION_ENTITY } from "./customer-groups-tabs";
import type { CustomerGroupsData } from "./types";
import { useDebouncedPayload } from "./use-debounced-payload";

/**
 * "Show changes" of the customer groups: the value as stored is JSON with ids only (customers by number,
 * users, groups and tasks by id), so the diff compares that, indented — the edited values as the server
 * would store them (`toJson`), the stored value and the former ones of the history of the row. Not
 * editable; the form is.
 */
export function CustomerGroupsDiffButton({
  data,
  isDirty,
}: {
  data: CustomerGroupsData;
  isDirty: boolean;
}) {
  const payload = useDebouncedPayload();
  const stored = prettyJson(data.storedJson);
  const edited = useQuery({
    queryKey: ["customerGroups", "toJson", payload],
    queryFn: ({ signal }) => customerGroupsToJson(payload, signal),
    enabled: isDirty,
    placeholderData: keepPreviousData,
  });
  const versions = useHistoryVersions({
    entity: CONFIGURATION_ENTITY,
    id: data.id,
    property: "stringValue",
    format: prettyJson,
  });
  return (
    <CodeDiffButton
      original={stored}
      current={
        isDirty && edited.data != null ? prettyJson(edited.data) : stored
      }
      language="json"
      versions={versions}
      labelled
    />
  );
}
