"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { addressViewQueryKey, fetchAddressView } from "@/lib/rs/address";
import type { AddressDetail } from "./types";

/**
 * The address as its view shows it (`address/view/{id}`), shared by the page and the dialog of the list; and the
 * refresh after its favorite star was toggled.
 */
export function useAddressView(id: number) {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: addressViewQueryKey(id),
    queryFn: ({ signal }) => fetchAddressView<AddressDetail>(id, signal),
  });
  return {
    query,
    refresh: () =>
      void queryClient.invalidateQueries({ queryKey: addressViewQueryKey(id) }),
  };
}
