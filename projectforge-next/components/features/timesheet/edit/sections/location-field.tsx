"use client";

import { useTranslations } from "next-intl";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { fetchLocationSuggestions } from "@/lib/rs/timesheet";

/**
 * Where the work was done, completing from the locations this user has booked before
 * (`isAutocompletionPropertyEnabled("location")`). A free string, not a reference — hence the
 * string-suggest field and not an autocomplete of entities.
 */
export function LocationField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <StringSuggestField
      name="location"
      label={t("timesheet.location")}
      className={className}
      suggest={fetchLocationSuggestions}
      // The completions depend on nothing but the term the user is typing.
      queryKey={["timesheet", "location"]}
    />
  );
}
