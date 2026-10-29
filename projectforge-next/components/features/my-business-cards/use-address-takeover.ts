"use client";

import { useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import type { EntityForm } from "@/components/shared/form/form-context";
import { toast } from "@/lib/toast";
import { fetchCardFromAddress } from "@/lib/rs/my-business-cards";
import { BUSINESS_CARD_FIELDS, type BusinessCard } from "./types";

const sourceKey = (addressId: number | null) =>
  ["myBusinessCards", "source", addressId] as const;

/**
 * The source address of a card: picking one takes its data over into the form (the card's name only if it has
 * none yet), and its data stays at hand to fill a field ticked again later ([source]).
 */
export function useAddressTakeover(
  form: EntityForm,
  initial: BusinessCard,
  addressId: number | null,
  onTakenOver: (card: BusinessCard) => void
) {
  const queryClient = useQueryClient();
  // Only the answer of the latest pick is taken over: picked twice in a row, the first answer may come last.
  const latestPick = useRef(0);
  const [address, setAddress] = useState<EntityRef | null>(
    initial.addressId
      ? {
          id: initial.addressId,
          displayName: [initial.firstName, initial.lastName]
            .filter(Boolean)
            .join(" "),
        }
      : null
  );
  const source = useQuery({
    queryKey: sourceKey(addressId),
    queryFn: ({ signal }) => fetchCardFromAddress(addressId!, signal),
    enabled: addressId != null,
  });
  const pick = async (ref: EntityRef | null) => {
    const pickNo = ++latestPick.current;
    setAddress(ref);
    form.setFieldValue("addressId", ref?.id ?? null);
    // The photo belongs to the address: another one may not be the own, so it is ticked again on purpose.
    form.setFieldValue("includePhoto", false);
    if (!ref) return;
    try {
      const card = await fetchCardFromAddress(ref.id);
      if (pickNo !== latestPick.current) return;
      queryClient.setQueryData(sourceKey(ref.id), card);
      for (const { field } of BUSINESS_CARD_FIELDS)
        form.setFieldValue(field, card[field] ?? null);
      if (!form.getFieldValue("cardName")?.trim())
        form.setFieldValue("cardName", card.cardName ?? "");
      onTakenOver(card);
    } catch (error) {
      if (pickNo !== latestPick.current) return;
      toast.error(error instanceof Error ? error.message : String(error));
    }
  };
  return { address, source: source.data, pick };
}
