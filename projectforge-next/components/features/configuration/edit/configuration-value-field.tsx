"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  fetchTimeZoneSuggestions,
  TIME_ZONE_SUGGEST_QUERY_KEY,
} from "@/lib/rs/configuration";
import type { ConfigurationValues } from "../schema";
import type { ConfigurationType } from "../types";
import { JSON_EDITORS } from "./json-editors";

/**
 * The one editable value of a configuration parameter, rendered as the input its `configurationType`
 * calls for — the polymorphic heart of the form, the counterpart of Wicket's `ConfigurationEditForm`
 * which switched on the same type.
 *
 * A custom field because the value is not one property with one data type: it lives in one of four
 * typed slots of the DTO (`stringValue`/`longValue`/`floatValue`/`booleanValue`), chosen by the type,
 * and the DO carries no `@PropertyInfo` field a metadata-driven field could bind to (all value fields
 * are `metadataLess`). The label and the ⓘ hint are the parameter's own runtime i18n keys — resolved
 * through [leafKeyOf] because a param key is both a text and the parent of its `.description`.
 */
export function ConfigurationValueField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const { configurationType, parameter, i18nKey, descriptionI18nKey } =
    useStore(form.store, (s: unknown) => (s as FormState).values);

  const label = i18nKey ? t(leafKeyOf(i18nKey, t.has)) : "";
  // The description key is a plain leaf, but a parameter that has none must not ask for a missing key.
  const hint =
    descriptionI18nKey && t.has(descriptionI18nKey)
      ? t(descriptionI18nKey)
      : undefined;

  return renderValueField(configurationType, parameter, label, hint, className);
}

/** Picks the input for the value slot the type stores in (see Configuration.copyTo). */
function renderValueField(
  type: ConfigurationType | null,
  parameter: string | null,
  label: string,
  hint: string | undefined,
  className?: string
) {
  switch (type) {
    case "JSON": {
      // A JSON parameter brings its own editor; one without is edited as text, checked by the backend.
      const Editor = parameter ? JSON_EDITORS[parameter] : undefined;
      if (Editor) {
        return <Editor label={label} hint={hint} className={className} />;
      }
      return (
        <TextAreaField
          name="stringValue"
          label={label}
          hint={hint}
          className={className}
          rows={10}
          metadataLess
        />
      );
    }
    case "TEXT":
      return (
        <TextAreaField
          name="stringValue"
          label={label}
          hint={hint}
          className={className}
          rows={6}
          metadataLess
        />
      );
    case "TIME_ZONE":
      return (
        <StringSuggestField
          name="stringValue"
          label={label}
          hint={hint}
          className={className}
          suggest={fetchTimeZoneSuggestions}
          queryKey={TIME_ZONE_SUGGEST_QUERY_KEY}
          metadataLess
        />
      );
    case "BOOLEAN":
      return (
        <CheckboxField
          name="booleanValue"
          label={label}
          hint={hint}
          className={className}
        />
      );
    case "LONG":
    case "INTEGER":
      return (
        <NumberField
          name="longValue"
          label={label}
          hint={hint}
          className={className}
          metadataLess
        />
      );
    case "FLOAT":
      return (
        <NumberField
          name="floatValue"
          label={label}
          hint={hint}
          className={className}
          fractionDigits={2}
          metadataLess
        />
      );
    case "PERCENT":
      // Stored as a factor (0.19), typed and read as a percentage (19) — the box does the conversion.
      return (
        <NumberField
          name="floatValue"
          label={label}
          hint={hint}
          className={className}
          percent
          suffix="%"
          metadataLess
        />
      );
    // STRING and everything the current parameter set does not use fall back to a plain line — the
    // value slot is `stringValue`, which the DTO writes for STRING (CALENDAR/TASK are not editable here).
    default:
      return (
        <InputField
          name="stringValue"
          label={label}
          hint={hint}
          className={className}
          metadataLess
        />
      );
  }
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<
    ConfigurationValues,
    "configurationType" | "parameter" | "i18nKey" | "descriptionI18nKey"
  >;
}
