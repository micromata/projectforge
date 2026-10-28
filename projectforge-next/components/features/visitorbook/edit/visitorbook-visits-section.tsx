"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
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
 * the list, which shows the latest visit, and the history are refreshed. With `?addVisit=1` the dialog of a new
 * visit opens right away; after a visit is saved, the list is shown again.
 */
export function VisitorbookVisitsSection({ id }: { id: number }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const ctx = useFormatContext();
  const router = useRouter();
  const visitor = useEntityDetail<VisitorbookDetail>("visitorbook", id).data;
  // `?addVisit=1`: reached to enter a visit (a stored visitor picked in a new one's name, or a visitor just added).
  // Dropped from the url once read, so a reload or the way back doesn't open the dialog again.
  const addVisit = useSearchParams().get("addVisit") === "1";
  useEffect(() => {
    if (addVisit) router.replace(`/visitorbook/${id}`);
  }, [addVisit, id, router]);

  return (
    <ValidityEntriesSection<VisitorbookEntry>
      entries={visitor?.entries ?? []}
      dateField="dateOfVisit"
      // Both keys are the parents of others too (the weekdays, `.arrive` and `.depart`), so their text is the `_` leaf.
      dateLabelKey="calendar.day._"
      valueFields={VALUE_FIELDS}
      dialogTitle={t("orga.visitorbook.timeofvisit._")}
      markValidity={false}
      openNewOnMount={addVisit}
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
      // Entering a visit is done once it is saved: back to the list, ready for the next visitor.
      onSaved={() => router.push("/visitorbook")}
    />
  );
}
