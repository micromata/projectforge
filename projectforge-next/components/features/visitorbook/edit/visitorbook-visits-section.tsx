"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ValidityEntriesSection } from "@/components/shared/validity-entries/validity-entries-section";
import type {
  ValidityEntriesAdapter,
  ValidityValueField,
} from "@/components/shared/validity-entries/types";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useFormatContext } from "@/hooks/use-format";
import { historyQueryKey } from "@/hooks/use-history";
import {
  deleteVisit,
  insertVisit,
  updateVisit,
} from "@/lib/rs/visitorbook-entry";
import { nowIso, todayOf, zonedPartsOf } from "@/lib/user-zone";
import { VISITORBOOK_LIST_QUERY_KEY } from "../visitorbook-query-key";
import type { VisitorbookDetail, VisitorbookEntry } from "../types";

const ADAPTER: ValidityEntriesAdapter<VisitorbookEntry> = {
  insert: insertVisit,
  update: updateVisit,
  remove: deleteVisit,
};

const VALUE_FIELDS: ValidityValueField[] = [
  {
    name: "arrived",
    labelKey: "orga.visitorbook.timeofvisit.arrive",
    kind: "text",
  },
  {
    name: "departed",
    labelKey: "orga.visitorbook.timeofvisit.depart",
    kind: "text",
  },
];

/**
 * The visits of a stored visitor, newest first. Written through `visitorbook/entry/...` right away, not with the
 * form (see ValidityEntriesSection); a visit is no validity period, so none is marked as the current one. The
 * answer of a write replaces the visits of the cached visitor (no refetch: it already is what the backend has);
 * the list, which shows the latest visit, and the history are refreshed.
 */
export function VisitorbookVisitsSection({ id }: { id: number }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const ctx = useFormatContext();
  const visitor = useEntityDetail<VisitorbookDetail>("visitorbook", id).data;

  return (
    <ValidityEntriesSection<VisitorbookEntry>
      entries={visitor?.entries ?? []}
      dateField="dateOfVisit"
      // Both keys are the parents of others too (the weekdays, `.arrive` and `.depart`), so their text is the `_` leaf.
      dateLabelKey="calendar.day._"
      valueFields={VALUE_FIELDS}
      dialogTitle={t("orga.visitorbook.timeofvisit._")}
      markValidity={false}
      // Today and now, as the legacy dialog did: a visit is usually entered as the visitor arrives.
      newEntry={() => ({
        id: null,
        dateOfVisit: todayOf(ctx),
        arrived: zonedPartsOf(nowIso(), ctx)?.time ?? null,
        visitorbookId: id,
      })}
      adapter={ADAPTER}
      // `null` is "not asked" and reads as allowed (lib/rs/entity-access.ts); the endpoints check anyway.
      readOnly={visitor?.writeAccess === false}
      onChanged={(entries) => {
        queryClient.setQueryData<VisitorbookDetail>(
          ["visitorbook", id],
          (old) => (old ? { ...old, entries } : old)
        );
        // The list shows the latest visit. Not the visitor itself: the answer above already is what the
        // backend has now.
        void queryClient.invalidateQueries({
          queryKey: VISITORBOOK_LIST_QUERY_KEY,
          predicate: (query) => query.queryKey[1] !== id,
        });
        void queryClient.invalidateQueries({
          queryKey: historyQueryKey("visitorbook", id),
        });
      }}
    />
  );
}
