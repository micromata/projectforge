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
 * Where leaving the address form leads: back to the view of the address it was opened from (the form is a sub
 * route of it), or to the list where there is none (a new one cancelled, one deleted). Opened for an entry of a
 * VCF import, every way leads back to the import, whose next entry is waiting; opened from the dialog of the list,
 * back to the list.
 *
 * @param id The address of the form, null while adding one.
 */
export function useAddressEditOutcome(id: number | null): Partial<EditOutcome> {
  const router = useRouter();
  const fromImport = importIndexOfUrl() !== null;
  // Opened from the dialog of the list (`?returnTo=/address`): back to the list, which the dialog was over.
  const toList = useSearchParams().get("returnTo") === ADDRESS_ROUTE;
  const back = (target: string) =>
    router.push(
      fromImport ? ADDRESS_IMPORT_ROUTE : toList ? ADDRESS_ROUTE : target
    );
  return {
    afterSave: (savedId) =>
      back(savedId != null ? addressViewRoute(savedId) : ADDRESS_ROUTE),
    afterCancel: () => back(id != null ? addressViewRoute(id) : ADDRESS_ROUTE),
    afterDelete: () => back(ADDRESS_ROUTE),
    afterUndelete: () =>
      back(id != null ? addressViewRoute(id) : ADDRESS_ROUTE),
  };
}
