"use client";

import { de, enGB, enUS, type Locale } from "react-day-picker/locale";
import { useFormatContext } from "@/hooks/use-format";

/**
 * The date-fns locale react-day-picker names its months and weekdays with, for the language of the
 * logged-in user.
 *
 * Only the app's own two languages are mapped (i18n/config.ts LOCALES); everything else falls back to
 * English, as the message catalogs do. The *first day of the week* deliberately does not come from
 * here — it is the user's own setting and is passed as `weekStartsOn`, which overrides the locale's
 * default (see FormatContext in lib/format.ts).
 */
export function useDatePickerLocale(): Locale {
  const { locale } = useFormatContext();
  if (locale.startsWith("de")) return de;
  return locale === "en-US" ? enUS : enGB;
}
