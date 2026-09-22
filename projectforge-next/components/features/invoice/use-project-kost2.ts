"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchActiveKost2 } from "@/lib/rs/invoice";
import type { InvoiceValues, KostZuweisungValues } from "./invoice-schema";

/**
 * The first active cost unit of the project the invoice **currently** names — what a new cost assignment
 * is preselected with where there is no predecessor row to take one from
 * (`RechnungCostEditTablePanel.newKostZuweisung`).
 *
 * Read from the live form value rather than from the persisted invoice: a user who just picked a project
 * and then adds a position expects the cost unit of *that* project, and on a new invoice there is no
 * persisted project at all.
 *
 * `undefined` while nothing is known — no project chosen, the read still on its way, or a project without
 * cost units. All three mean the same thing for the caller: no proposal, so the field starts empty. So is
 * an answer without an id, which the form could not write back anyway (`Rechnung.copyTo` resolves the cost
 * unit by it).
 */
export function useProjectKost2(): KostZuweisungValues["kost2"] | undefined {
  const form = useEntityEditForm();
  const projektId = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as InvoiceValues).project?.id ?? null
  ) as number | null;

  const query = useQuery({
    queryKey: ["outgoingInvoice", "activeKost2", projektId],
    queryFn: ({ signal }) => fetchActiveKost2(projektId as number, signal),
    enabled: projektId != null,
    // The cost units of a project change when someone edits the cost unit master data, not while an
    // invoice is being written.
    staleTime: 10 * 60_000,
  });
  const first = query.data?.[0];
  return first?.id == null
    ? undefined
    : { id: first.id, displayName: first.displayName ?? undefined };
}
