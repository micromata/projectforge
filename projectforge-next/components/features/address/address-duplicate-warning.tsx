"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { fetchDuplicateAddresses } from "@/lib/rs/address";
import type { AddressValues } from "./address-schema";

/**
 * While an address is being added: the addresses that have the name and first name typed so far already, each a
 * link to its view — so the user notices the person is there before entering them twice.
 */
export function AddressDuplicateWarning() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const names = useStore(form.store, (s: unknown) => {
    const values = (s as { values: AddressValues }).values;
    return `${values.name?.trim() ?? ""}\u0000${values.firstName?.trim() ?? ""}`;
  });
  const isNew = useStore(
    form.store,
    (s: unknown) => (s as { values: AddressValues }).values.id == null
  );
  const debounced = useDebouncedValue(names, 500);
  const [name, firstName] = debounced.split("\u0000");
  const query = useQuery({
    queryKey: ["address", "duplicates", name, firstName],
    queryFn: ({ signal }) => fetchDuplicateAddresses(name, firstName, signal),
    enabled: isNew && (name !== "" || firstName !== ""),
  });
  const duplicates = isNew ? (query.data ?? []) : [];
  if (duplicates.length === 0) return null;
  return (
    <FormAlert tone="info" className="border-warning/50 bg-warning/10">
      <p className="font-semibold">{t("address.validation.duplicateFound")}</p>
      <p>{t("address.validation.duplicateWarning")}</p>
      <ul className="mt-1 list-disc pl-5">
        {duplicates.map((address) => (
          <li key={address.id}>
            <Link
              href={`/address/${address.id}`}
              target="_blank"
              className="underline"
            >
              {address.fullName}
            </Link>
            {/* The full name ends with the organization already. */}
            {address.email && `, ${address.email}`}
          </li>
        ))}
      </ul>
    </FormAlert>
  );
}
