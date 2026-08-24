"use client";

import { useTranslations } from "next-intl";
import {
  INTEGER,
  REQUIRED,
  parseI18nMarker,
  parseMaxLengthMarker,
  parseMaxMarker,
  parseMinMarker,
} from "@/lib/validation/markers";

/** The slice of a field's meta state the error texts are built from. */
export interface FieldErrorMeta {
  errors?: unknown[];
}

/**
 * Turns the errors of a field into displayable texts.
 *
 * Two shapes arrive here. A plain string is the server's message (see lib/validation/server-errors.ts),
 * already translated by the backend, and is shown verbatim. An object is a Zod issue, whose `message`
 * is one of our own markers (lib/validation/markers.ts) — they become the backend's own wording, with
 * the field's label and, for a length, the limit as arguments. Anything else is Zod's own English
 * default ("Invalid input: expected string, received undefined"): a schema bug, never something a user
 * should read, so it turns into the generic message and is logged.
 */
export function useFieldErrors(): (
  meta: FieldErrorMeta,
  label: string
) => string[] {
  const t = useTranslations();
  return (meta, label) =>
    (meta.errors ?? [])
      .map((e) => {
        if (e == null) return null;
        if (typeof e === "string") return e;
        if (typeof e !== "object" || !("message" in e)) return null;
        const message = String((e as { message?: unknown }).message ?? "");
        if (message === REQUIRED)
          return t("validation.error.fieldRequired", { arg0: label });
        if (message === INTEGER) return t("validation.error.format.integer");
        // A rule the bundle words completely on its own — see i18nMarker.
        const i18nKey = parseI18nMarker(message);
        if (i18nKey !== null) return t(i18nKey);
        const maxLength = parseMaxLengthMarker(message);
        if (maxLength !== null)
          return t("validation.error.maxLength", {
            arg0: label,
            arg1: maxLength,
          });
        // The range messages name the bound, not the field: they sit under a segment of the number
        // group, where the label of the whole group is already visible above (see
        // SegmentedNumberField).
        const min = parseMinMarker(message);
        if (min !== null)
          return t("validation.error.range.integerToLow", { arg0: min });
        const max = parseMaxMarker(message);
        if (max !== null)
          return t("validation.error.range.integerToHigh", { arg0: max });
        console.warn(`Untranslated validation error on "${label}": ${message}`);
        return t("validation.error.generic");
      })
      .filter((m): m is string => !!m);
}
