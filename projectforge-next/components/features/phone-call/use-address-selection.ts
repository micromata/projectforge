"use client";

import { useCallback, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchAddress, suggestNumbers } from "@/lib/rs/phone-call";
import type { AcItem, AddressInfo } from "./types";

/**
 * Bridges the string-based [SuggestInput] to the address behind a picked number, so the address panel follows
 * the selection instead of staying on the deep-linked contact.
 *
 * [SuggestInput] deals in plain strings: it shows and commits the `display` text. Each lookup is cached here by
 * both its `display` and its clean `number`, so committing either (picking a row, or blurring on the number the
 * pick left behind) finds the same [AcItem]. A hit with an `addressId` refreshes the panel; a recent number
 * (none) clears it; free text the box never suggested leaves both untouched.
 */
export function useAddressSelection(
  initialAddress: AddressInfo | null,
  setPhoneNumber: (value: string) => void
) {
  // The picked address as {id, number}: the number is passed to fetchAddress so the backend can remember the
  // pair as the last shown address (restored on re-open).
  const [selected, setSelected] = useState<{
    id: number;
    number: string;
  } | null>(initialAddress ? { id: initialAddress.id, number: "" } : null);
  const itemsRef = useRef(new Map<string, AcItem>());

  const suggest = useCallback(async (search: string, signal?: AbortSignal) => {
    const items = await suggestNumbers(search, signal);
    const map = itemsRef.current;
    items.forEach((item) => {
      map.set(item.display, item);
      map.set(item.number, item);
    });
    return items.map((item) => item.display);
  }, []);

  const onCommit = useCallback(
    (committed: string) => {
      const item = itemsRef.current.get(committed);
      if (!item) return;
      // Replace the "<number>: name, ..." label the box committed with the clean number to dial.
      setPhoneNumber(item.number);
      setSelected(
        item.addressId != null
          ? { id: item.addressId, number: item.number }
          : null
      );
    },
    [setPhoneNumber]
  );

  const addressQuery = useQuery({
    queryKey: ["phoneCall-address", selected?.id, selected?.number],
    queryFn: ({ signal }) =>
      fetchAddress(selected!.id, selected!.number, signal),
    // The deep-linked address is already in hand; only a newly picked one needs fetching.
    enabled: selected != null && selected.id !== (initialAddress?.id ?? null),
  });

  const activeAddress =
    selected == null
      ? null
      : selected.id === initialAddress?.id
        ? initialAddress
        : (addressQuery.data ?? null);

  return { suggest, onCommit, activeAddress };
}
