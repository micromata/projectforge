"use client";

import type { DynamicComponentProps } from "../../dynamic-renderer";
import { DynamicFallback } from "../dynamic-fallback";
import { DynamicAutoCompleteInput } from "./dynamic-autocomplete-input";
import { DynamicDateInput } from "./dynamic-date-input";
import { DynamicEntityInput } from "./dynamic-entity-input";
import { DynamicTextInput } from "./dynamic-text-input";
import { DynamicCheckbox } from "../dynamic-checkbox";

/**
 * Picks the input for an INPUT element from its `dataType` (org.projectforge.ui.UIDataType).
 *
 * The layout uses one element type for everything a user types into, so the concrete control is
 * only known from the data type - mirroring DynamicInputResolver.jsx of the legacy renderer.
 */
export function DynamicInputResolver({ node }: DynamicComponentProps) {
  const dataType = (node.dataType as string) ?? "STRING";

  switch (dataType) {
    case "STRING":
      // The backend offers suggestions for some free-text fields (UIInput.enableAutoCompletion).
      return node.autoCompletionUrl ? (
        <DynamicAutoCompleteInput node={node} />
      ) : (
        <DynamicTextInput node={node} />
      );
    case "PASSWORD":
      return <DynamicTextInput node={node} inputType="password" />;
    case "INT":
    case "LONG":
    case "DECIMAL":
    case "AMOUNT":
      return <DynamicTextInput node={node} inputType="number" />;
    case "BOOLEAN":
      return <DynamicCheckbox node={node} />;
    case "DATE":
      return <DynamicDateInput node={node} />;
    case "TIME":
      return <DynamicTextInput node={node} inputType="time" />;
    case "TIMESTAMP":
      return <DynamicTextInput node={node} inputType="datetime-local" />;
    case "USER":
    case "GROUP":
    case "EMPLOYEE":
    case "COST1":
    case "COST2":
    case "KONTO":
      // A reference to another entry, searched for by name - the same set the legacy renderer sends
      // to its ObjectSelect, and the same `{type}/autosearch` endpoints.
      return <DynamicEntityInput node={node} />;
    default:
      // TASK, LOCALE, TIMEZONE, PICTURE, CUSTOMIZED: each needs a picker of its own (the task its
      // tree, a picture its upload), which is not part of the current migration step.
      return <DynamicFallback node={node} />;
  }
}
