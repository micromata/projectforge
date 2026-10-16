"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import {
  addressImportEntryQueryKey,
  fetchAddressImportEntry,
} from "@/lib/rs/address-import";
import { importIndexOfUrl } from "./address-values";
import { AddressDuplicateWarning } from "./address-duplicate-warning";
import { AddressTextParser } from "./text-parser/address-text-parser";

/**
 * Above the sections of the address form (see EditDef.editIntro): the warning about addresses with the same name
 * while adding one, and the text parser — which compares the fields of the vCard if the form was opened for an
 * entry of a VCF import (`?importIndex=`, see the import page).
 */
export function AddressEditIntro() {
  const t = useTranslations();
  const importIndex = importIndexOfUrl();
  const entry = useQuery({
    queryKey: addressImportEntryQueryKey(importIndex ?? -1),
    queryFn: ({ signal }) => fetchAddressImportEntry(importIndex!, signal),
    enabled: importIndex !== null,
    // The vCard of the entry stays what it is; its comparison is read once per visit of the form. Read anew on
    // every visit though (dropped from the cache on leaving): reading it stages the entry's image in the session,
    // whose single slot another entry may have taken meanwhile — and a new upload replaces the entries.
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
  });
  return (
    <div className="flex flex-col gap-3">
      <AddressDuplicateWarning />
      {entry.isError && (
        // The import is kept in the session for half an hour; afterwards the entry is gone.
        <FormAlert tone="error">{t("address.import.entryExpired")}</FormAlert>
      )}
      <AddressTextParser importEntry={entry.data} />
    </div>
  );
}
