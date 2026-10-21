"use client";

import { useCallback } from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type {
  MerlinTemplate,
  MerlinVariable,
  MerlinVariablesResult,
} from "@/lib/rs/merlin";

/**
 * The variables of the form being edited, and the way to replace them with what the backend computed
 * (`merlin/analyze`, `merlin/variable/update`/`delete`): the variables are never edited field by field,
 * a change of one of them may move it from the input to the dependent variables, rename the master of
 * others and change their usage.
 */
export function useMerlinVariables() {
  const form = useEntityEditForm();
  const variables = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values.variables ?? []) as MerlinVariable[]
  );
  const dependentVariables = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values.dependentVariables ?? []) as MerlinVariable[]
  );

  /** The template as it is in the form, for posting it. */
  const currentTemplate = useCallback(
    () => form.state.values as MerlinTemplate,
    [form]
  );

  /**
   * Takes over the variables (and, after an analysis, the file names and conditionals). A change of a
   * variable marks the form dirty, as it is saved with the form. An analysis doesn't: it shows what the
   * stored template has (its attachments are saved at once), as loading the form does.
   */
  const apply = useCallback(
    (result: MerlinVariablesResult, analysis = false) => {
      const set = (name: string, value: unknown) =>
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        form.setFieldValue(name as any, value as any, {
          dontUpdateMeta: analysis,
        });
      set("variables", result.variables);
      set("dependentVariables", result.dependentVariables);
      if (result.lastVariableUpdate !== undefined) {
        set("lastVariableUpdate", result.lastVariableUpdate);
      }
      if (analysis) {
        set("wordTemplateFileName", result.wordTemplateFileName ?? null);
        set(
          "excelTemplateDefinitionFileName",
          result.excelTemplateDefinitionFileName ?? null
        );
        set("conditionalsMarkdown", result.conditionalsMarkdown ?? null);
      }
    },
    [form]
  );

  return { variables, dependentVariables, currentTemplate, apply };
}

/**
 * How a variable is marked, as in the legacy page: a master variable (others depend on it, or a
 * conditional uses it) red, an unused one muted, an input variable green, a dependent one neutral.
 */
export function variableTone(
  variable: MerlinVariable
): "danger" | "neutral" | "success" | "info" {
  if (variable.masterVariable) return "danger";
  if (variable.used === false) return "neutral";
  if (variable.dependsOnName) return "info";
  return "success";
}
