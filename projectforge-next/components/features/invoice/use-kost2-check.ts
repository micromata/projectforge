"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchKost2Check } from "@/lib/rs/invoice";
import type { InvoiceValues } from "./invoice-schema";

/**
 * Whether a cost unit belongs to the project — or, where the invoice names none, to the customer — the
 * invoice is written for. False is what the form warns about, as Wicket outlines the field
 * (`RechnungEditForm.onRenderCostRow`).
 *
 * The comparison is the backend's (`outgoingInvoice/kost2Check`): it is over the number range, the area and
 * the number of the project or the customer, and the invoice carries neither with those fields on it.
 *
 * Project and customer come from the **live** form value, like [useProjectKost2]: picking a different
 * project has to change the answer at once, and a new invoice has no persisted one at all.
 *
 * True while nothing is known — no cost unit chosen, or the read still on its way. A warning has to be
 * earned: shown before the answer arrives it would blink at every row of a freshly opened invoice.
 */
export function useKost2Check(kost2Id: number | null | undefined): boolean {
  const form = useEntityEditForm();
  // A string, so `useStore` compares by value: the two ids are what the query depends on, while the
  // objects around them are new on every keystroke elsewhere in the form.
  const owner = useStore(form.store, (state) => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const values = (state as any).values as InvoiceValues;
    return `${values.project?.id ?? ""}/${values.customer?.id ?? ""}`;
  }) as string;
  const [projektId, kundeId] = owner.split("/");

  const query = useQuery({
    queryKey: ["outgoingInvoice", "kost2Check", kost2Id, projektId, kundeId],
    queryFn: ({ signal }) =>
      fetchKost2Check(
        kost2Id as number,
        projektId ? Number(projektId) : null,
        kundeId ? Number(kundeId) : null,
        signal
      ),
    enabled: kost2Id != null,
    // The answer follows from cost unit master data and the invoice's own project, so it only changes when
    // one of the query key's parts does.
    staleTime: 10 * 60_000,
  });

  return query.data?.matchesInvoice ?? true;
}
