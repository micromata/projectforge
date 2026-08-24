"use client";

import { useStore } from "@tanstack/react-form";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import {
  recalculateInvoiceSums,
  type InvoicePositionSums,
  type InvoiceSums,
} from "@/lib/rs/invoice-sums";

/** The form fields the recalculated sums actually depend on, whichever invoice kind holds them. */
interface SumsRelevantValues {
  id?: number | null;
  status?: string | null;
  datum?: string | null;
  faelligkeit?: string | null;
  bezahlDatum?: string | null;
  zahlBetrag?: number | null;
  discountPercent?: number | null;
  discountMaturity?: string | null;
  positionen?: {
    id?: number | null;
    number?: number | null;
    deleted?: boolean;
    menge?: number | null;
    einzelNetto?: number | null;
    vat?: number | null;
    kostZuweisungen?: unknown;
  }[];
}

/**
 * What the answer depends on, and nothing else: the positions with their cost assignments, and the
 * fields the discount and the overdue flag follow from.
 *
 * Recalculating whenever the subject changes would ask the server about something that cannot move the
 * numbers, so the query is keyed by this slice rather than by the whole form. `status` is read where a
 * form has it (the outgoing invoice) and left out silently where it does not (the incoming one).
 */
function sumsInput(values: SumsRelevantValues) {
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
    positionen: (values.positionen ?? []).map((pos) => ({
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
  sums: InvoiceSums | undefined;
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
 * An invoice's sums as the server computes them, following the form while it is being edited.
 *
 * Deliberately not computed here: how a position is rounded before it enters a sum is German law and
 * `RechnungCalculator`'s rule (`roundPositionsBeforeSum = true`), and how much of a net sum is still
 * unassigned follows from it. A second implementation in the browser would be a second answer. So the
 * form state goes to `POST /rs/{entity}/recalculate` and the numbers come back (see lib/rs/invoice-sums).
 *
 * Debounced, because this is a read of something the user is still typing: every intermediate amount
 * would otherwise be a request of its own.
 *
 * @param entity The REST category of the invoice, e.g. `outgoingInvoice` or `incomingInvoice` — it keys
 *   the query and picks the endpoint, so the two forms never share a cache entry.
 */
export function useInvoiceSums(entity: string): InvoiceSumsState {
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) =>
      JSON.stringify(sumsInput(state.values as SumsRelevantValues))
  ) as string;
  // The serialised slice is what is debounced and what keys the query: the selector builds a new object
  // on every render, so an object identity would never settle and the debounce would never fire.
  const key = useDebouncedValue(values, 400);

  const query = useQuery({
    queryKey: [entity, "recalculate", key],
    queryFn: ({ signal }) =>
      recalculateInvoiceSums(entity, JSON.parse(key), signal),
    // The answer is a pure function of what was sent, so it stays valid while the user edits elsewhere.
    staleTime: 60_000,
    // The previous answer stands while the next one is computed: every keystroke is a new key, so
    // without this the sums blank out on and off while a position is being typed — and the line showing
    // them dims itself for exactly this moment (see [InvoiceSumsLine]), which only reads as "being
    // recomputed" if the numbers are still there. A cost assignment's percentage entry needs it as more
    // than polish: the net sum it takes its share of comes from here.
    placeholderData: keepPreviousData,
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
