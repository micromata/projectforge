"use client";

import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { VISITORBOOK_METADATA } from "@/lib/metadata/visitorbook.generated";
import { fetchCompanySuggestions } from "@/lib/rs/visitorbook";

/** The company of the visitor, completing from the company names the other visitors already have. */
export function VisitorbookCompanyField({ className }: { className?: string }) {
  const label = useFieldLabels(VISITORBOOK_METADATA);
  return (
    <StringSuggestField
      name="company"
      label={label("company")}
      className={className}
      suggest={fetchCompanySuggestions}
      queryKey={["visitorbook", "autocomplete", "company"]}
    />
  );
}
