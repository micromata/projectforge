"use client";

import { useMemo } from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import type { CustomerGroupsValues } from "./types";
import { toPayload } from "./values";

/** As the live validation: a name typed letter by letter is asked about once. */
const DELAY_MILLIS = 500;

/**
 * The unsaved values as the server takes them, settled for a moment: for the evaluations asked after
 * every change (`unassigned`, `businessUnitMembers`), as query key and request body.
 */
export function useDebouncedPayload(): CustomerGroupsValues {
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    (s: unknown) => (s as { values: CustomerGroupsValues }).values
  );
  const debounced = useDebouncedValue(values, DELAY_MILLIS);
  return useMemo(() => toPayload(debounced), [debounced]);
}
