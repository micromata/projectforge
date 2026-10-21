"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { fetchExistingVisitors } from "@/lib/rs/visitorbook";
import type { VisitorbookValues } from "../visitorbook-schema";

/** Characters to type (first and last name together) before the stored visitors are looked up. */
const MIN_CHARS = 2;

/**
 * The stored visitors whose names start with the first and last name typed into the form, while a visitor is being
 * added (none for a stored one). Asked only while [active], i.e. while a name field has the focus.
 */
export function useExistingVisitors(active: boolean) {
  const form = useEntityEditForm();
  const names = useStore(form.store, (s: unknown) => {
    const values = (s as { values: VisitorbookValues }).values;
    return `${values.firstname?.trim() ?? ""}\u0000${values.lastname?.trim() ?? ""}`;
  });
  const isNew = useStore(
    form.store,
    (s: unknown) => (s as { values: VisitorbookValues }).values.id == null
  );
  const [firstname, lastname] = useDebouncedValue(names, 300).split("\u0000");
  const enough = firstname.length + lastname.length >= MIN_CHARS;
  const { data = [] } = useQuery({
    queryKey: ["visitorbook", "existing", firstname, lastname],
    queryFn: ({ signal }) => fetchExistingVisitors(firstname, lastname, signal),
    enabled: isNew && active && enough,
    // Keeps the list in place while the next term is asked, instead of emptying and refilling it on every key.
    placeholderData: keepPreviousData,
  });
  // Below the minimum the kept list of a longer term would linger.
  return { existing: isNew && enough ? data : [], isNew };
}
