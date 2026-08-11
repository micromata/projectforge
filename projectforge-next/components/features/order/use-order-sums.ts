"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { recalculateOrder, type OrderPositionSums } from "@/lib/rs/order";
import type { OrderPositionValues, OrderValues } from "./order-schema";

/**
 * What the sums depend on, and nothing else: the positions' amounts and statuses, the payment schedule,
 * and the order's own status and probability of occurrence.
 *
 * Recalculating whenever the title changes would ask the server about something that cannot move the
 * numbers, so the query is keyed by this slice rather than by the whole form.
 */
function sumsInput(values: OrderValues) {
  return {
    id: values.id,
    status: values.status,
    probabilityOfOccurrence: values.probabilityOfOccurrence,
    // Deleted rows are sent along: whether they count is `OrderInfo.calculateAll`'s decision (they
    // don't), not something to be silently filtered out here.
    positionen: values.positionen.map((pos: OrderPositionValues) => ({
      id: pos.id,
      number: pos.number,
      deleted: pos.deleted,
      nettoSumme: pos.nettoSumme,
      personDays: pos.personDays,
      status: pos.status,
      vollstaendigFakturiert: pos.vollstaendigFakturiert,
    })),
    paymentSchedules: values.paymentSchedules,
  };
}

export interface OrderSumsState {
  /** Sums of the whole order; undefined while the first answer is on its way. */
  sums: Awaited<ReturnType<typeof recalculateOrder>> | undefined;
  /** Sums of one position by its number — a new position has no number yet, hence no entry. */
  positionSums: (
    number: number | null | undefined
  ) => OrderPositionSums | undefined;
  isLoading: boolean;
}

/**
 * The order's sums as the server computes them, following the form while it is being edited.
 *
 * Deliberately not computed here: which statuses count as commissioned, how a probability of occurrence
 * weighs into the acquisition sum and which positions are already invoiced is the business logic of
 * `OrderInfo` — a second implementation in the browser would be a second answer. So the form state goes
 * to `/rs/order/recalculate` and the numbers come back (see lib/rs/order.ts).
 *
 * Debounced, because this is a read of something the user is still typing: every intermediate amount
 * would otherwise be a request of its own.
 */
export function useOrderSums(): OrderSumsState {
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => JSON.stringify(sumsInput(state.values as OrderValues))
  ) as string;
  // The serialised slice is what is debounced and what keys the query: the selector builds a new object
  // on every render, so an object identity would never settle and the debounce would never fire.
  const key = useDebouncedValue(values, 400);

  const query = useQuery({
    queryKey: ["order", "recalculate", key],
    queryFn: ({ signal }) => recalculateOrder(JSON.parse(key), signal),
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
