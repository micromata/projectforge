"use client";

import { useEffect } from "react";
import { useForm } from "@tanstack/react-form";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  executeMerlinTemplate,
  loadMerlinExecution,
  merlinExecutionQueryKey,
  type MerlinExecution,
  type MerlinInputVariable,
  type MerlinExecutionValues,
} from "@/lib/rs/merlin";
import { toast } from "@/lib/toast";
import {
  applyServerValidationErrors,
  clearServerValidationErrors,
} from "@/lib/validation/server-errors";
import { inputFieldName } from "./merlin-execute-metadata";

/** A number stored as text by the legacy page ("40") becomes a number for the number field. */
function storedValue(variable: MerlinInputVariable, value: unknown): unknown {
  if (value == null || value === "") return null;
  if (
    (variable.type === "INT" || variable.type === "FLOAT") &&
    !variable.allowedValues?.length
  ) {
    const number = typeof value === "number" ? value : Number(value);
    return Number.isFinite(number) ? number : null;
  }
  return value;
}

function toValues(execution: MerlinExecution): MerlinExecutionValues {
  const stored = execution.inputVariables ?? {};
  return {
    id: execution.id,
    name: execution.name ?? null,
    pdfFormat: execution.pdfFormat,
    // Every variable has a value, so no field is uncontrolled; the stored ones are the user's last input.
    inputVariables: Object.fromEntries(
      execution.variables.map((it) => [
        it.name,
        storedValue(it, stored[it.name]),
      ])
    ),
  };
}

/**
 * The execution of a template: the form of its input variables (prefilled with the user's last input,
 * stored by the backend with every execution) and the execution itself, which downloads the document. A
 * refused value comes back as HTTP 406 and is shown at its field.
 */
export function useMerlinExecution(id: number) {
  const load = useQuery({
    queryKey: merlinExecutionQueryKey(id),
    queryFn: ({ signal }) => loadMerlinExecution(id, signal),
    // Refetched, the form would be reset onto the stored values and lose what the user entered.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });

  const execution = useMutation({
    mutationFn: (values: MerlinExecutionValues) =>
      executeMerlinTemplate(values),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const form = useForm({
    defaultValues: {
      id,
      pdfFormat: false,
      inputVariables: {},
    } as MerlinExecutionValues,
    onSubmit: async ({ value }) => {
      const result = await execution.mutateAsync(value).catch(() => undefined);
      if (result?.kind !== "validationErrors") return;
      const known = (load.data?.variables ?? []).map(inputFieldName);
      const { unassigned } = applyServerValidationErrors(
        form,
        result.validationErrors,
        known
      );
      unassigned.forEach((message) => toast.error(message));
    },
  });

  useEffect(() => {
    if (load.data) form.reset(toValues(load.data));
  }, [load.data, form]);

  /**
   * Executes. The backend is the only validator here (406 at the fields), and this form has no validator of
   * its own that would clear a field's server error once it is edited — so the errors of the previous answer
   * are cleared first, or they would block this try before it is sent.
   */
  const submit = () => {
    clearServerValidationErrors(form);
    void form.handleSubmit();
  };

  return { form, load, execution, submit };
}
