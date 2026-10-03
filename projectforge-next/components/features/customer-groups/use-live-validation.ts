"use client";

import { useEffect, useMemo } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import type { EntityForm } from "@/components/shared/form/form-context";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { validateCustomerGroups } from "@/lib/rs/customer-groups";
import { applyServerValidationErrors } from "@/lib/validation/server-errors";
import { CUSTOMER_GROUPS_FIELDS } from "./schema";
import type { CustomerGroupsValues } from "./types";
import { toPayload } from "./values";

/** Long enough for a name typed letter by letter to be asked about once. */
const DELAY_MILLIS = 500;

/**
 * Has the server validate the unsaved configuration shortly after every change and puts its errors on the
 * fields, as a save would — a conflict (`dhl*` here, `dh*` in another group) shows up while it is made, on
 * both sets involved: on the one being edited and on the other one, whose row opens by itself.
 *
 * The rules are the server's alone (customers and free texts in use, the task tree), so there is no client
 * copy of them to keep in step. Errors naming no field (the configuration grown too large) are left to the
 * save, which toasts them.
 */
export function useLiveValidation(form: EntityForm, isDirty: boolean) {
  const values = useStore(
    form.store,
    (s: unknown) => (s as { values: CustomerGroupsValues }).values
  );
  const debounced = useDebouncedValue(values, DELAY_MILLIS);
  const payload = useMemo(() => toPayload(debounced), [debounced]);
  const { data } = useQuery({
    queryKey: ["customerGroups", "validate", payload],
    queryFn: ({ signal }) => validateCustomerGroups(payload, signal),
    enabled: isDirty,
    placeholderData: keepPreviousData,
  });

  // Pushes the answer into the form's error slots: the form state is not the query's to own.
  useEffect(() => {
    if (!data || !isDirty) return;
    applyServerValidationErrors(
      form,
      data,
      CUSTOMER_GROUPS_FIELDS,
      CUSTOMER_GROUPS_FIELDS
    );
    // The fields show an error once touched only; the other set of a conflict wasn't.
    data.forEach(({ fieldId }) => {
      if (fieldId && form.getFieldMeta(fieldId)) {
        form.setFieldMeta(fieldId, (prev: object) => ({
          ...prev,
          isTouched: true,
        }));
      }
    });
  }, [data, isDirty, form]);
}
