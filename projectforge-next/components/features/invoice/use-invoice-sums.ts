"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { recalculateInvoice, type InvoicePositionSums } from "@/lib/rs/invoice";
import type { InvoicePositionValues, InvoiceValues } from "./invoice-schema";

/**
 * What the answer depends on, and nothing else: the positions with their cost assignments, and the four
 * fields the discount and the overdue flag follow from.
 *
 * Recalculating whenever the subject changes would ask the server about something that cannot move the
 * numbers, so the query is keyed by this slice rather than by the whole form.
 */
function sumsInput(values: InvoiceValues) {
  return {
    id: values.id,
    status: values.status,
    datum: values.datum,
    faelligkeit: values.faelligkeit,
    bezahlDatum: values.bezahlDatum,
    zahlBetrag: values.zahlBetrag,
    discountPercent: values.discountPercent,
    discountMaturity: values.discountMaturity,
    // Deleted rows are sent along: whether they count is `RechnungCalculator`'s decision (they don't),
    // not something to be silently filtered out here.
    positionen: values.positionen.map((pos: InvoicePositionValues) => ({
      id: pos.id,
      number: pos.number,
      deleted: pos.deleted,
      menge: pos.menge,
      einzelNetto: pos.einzelNetto,
      vat: pos.vat,
      // Sent whole: the Fehlbetrag of a position is the difference between its net sum and the sum of
      // these, so an amount typed into one of them moves two numbers of the answer.
      kostZuweisungen: pos.kostZuweisungen,
    })),
  };
}

export interface InvoiceSumsState {
  /** Sums of the whole invoice; undefined while the first answer is on its way. */
  sums: Awaited<ReturnType<typeof recalculateInvoice>> | undefined;
  /**
   * Sums of one position by its number. A deleted position has none — `RechnungCalculator` skips it — and
   * neither has one whose number the form hasn't assigned yet.
   */
  positionSums: (
    number: number | null | undefined
  ) => InvoicePositionSums | undefined;
  isLoading: boolean;
}

/**
 * The invoice's sums as the server computes them, following the form while it is being edited.
 *
 * Deliberately not computed here: how a position is rounded before it enters a sum is German law and
 * `RechnungCalculator`'s rule (`roundPositionsBeforeSum = true`), and how much of a net sum is still
 * unassigned follows from it. A second implementation in the browser would be a second answer. So the
 * form state goes to `POST /rs/outgoingInvoice/recalculate` and the numbers come back (see
 * lib/rs/invoice.ts).
 *
 * Debounced, because this is a read of something the user is still typing: every intermediate amount
 * would otherwise be a request of its own.
 */
export function useInvoiceSums(): InvoiceSumsState {
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => JSON.stringify(sumsInput(state.values as InvoiceValues))
  ) as string;
  // The serialised slice is what is debounced and what keys the query: the selector builds a new object
  // on every render, so an object identity would never settle and the debounce would never fire.
  const key = useDebouncedValue(values, 400);

  const query = useQuery({
    queryKey: ["outgoingInvoice", "recalculate", key],
    queryFn: ({ signal }) => recalculateInvoice(JSON.parse(key), signal),
    // The answer is a pure function of what was sent, so it stays valid while the user edits elsewhere.
    staleTime: 60_000,
  });

  const byNumber = new Map(
    (query.data?.positions ?? []).map((pos) => [pos.number, pos])
  );
  return {
    sums: query.data,
    positionSums: (number) =>
      number == null ? undefined : byNumber.get(number),
    isLoading: query.isFetching,
  };
}
