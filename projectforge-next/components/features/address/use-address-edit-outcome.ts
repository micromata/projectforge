"use client";

import { useRouter, useSearchParams } from "next/navigation";
import type { EditOutcome } from "@/components/shared/edit/edit-outcome";
import {
  ADDRESS_IMPORT_ROUTE,
  ADDRESS_ROUTE,
  addressViewRoute,
} from "./address-routes";
import { importIndexOfUrl } from "./address-values";

/**
 * The lists that open an address in their dialog (AddressViewDialog) and are returned to from its form — the
 * whitelist of `?returnTo=`, in line with `ADDRESS_PAGE.edit.returnTargets`.
 */
const DIALOG_LISTS: readonly string[] = [
  ADDRESS_ROUTE,
  "/address-campaign-value",
];

/**
 * Where leaving the address form leads: back to the view of the address it was opened from (the form is a sub
 * route of it), or to the list where there is none (a new one cancelled, one deleted). Opened for an entry of a
 * VCF import, every way leads back to the import, whose next entry is waiting; opened from the dialog of a list,
 * back to that list.
 *
 * @param id The address of the form, null while adding one.
 */
export function useAddressEditOutcome(id: number | null): Partial<EditOutcome> {
  const router = useRouter();
  const fromImport = importIndexOfUrl() !== null;
  // Opened from the dialog over a list (`?returnTo=/address`, or the address campaign values): back to that list.
  const returnTo = useSearchParams().get("returnTo");
  const toList =
    returnTo != null && DIALOG_LISTS.includes(returnTo) ? returnTo : null;
  const back = (target: string) =>
    router.push(fromImport ? ADDRESS_IMPORT_ROUTE : (toList ?? target));
  return {
    afterSave: (savedId) =>
      back(savedId != null ? addressViewRoute(savedId) : ADDRESS_ROUTE),
    afterCancel: () => back(id != null ? addressViewRoute(id) : ADDRESS_ROUTE),
    afterDelete: () => back(ADDRESS_ROUTE),
    afterUndelete: () =>
      back(id != null ? addressViewRoute(id) : ADDRESS_ROUTE),
  };
}
