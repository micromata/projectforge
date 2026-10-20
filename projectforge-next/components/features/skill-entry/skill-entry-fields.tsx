"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { RatingField } from "@/components/shared/form/rating-field";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { SKILL_ENTRY_METADATA } from "@/lib/metadata/skill-entry.generated";
import { fetchSkillSuggestions } from "@/lib/rs/skill-entry";
import type { SkillEntryValues } from "./skill-entry-schema";

/**
 * The skill, completing from the skills anybody has already entered — the autocompletion of the legacy
 * React form (`UIInput.enableAutoCompletion`), so the same skill is not spelled in ten ways.
 */
export function SkillField({ className }: { className?: string }) {
  const label = useFieldLabels(SKILL_ENTRY_METADATA);
  return (
    <StringSuggestField
      name="skill"
      label={label("skill")}
      className={className}
      suggest={fetchSkillSuggestions}
      queryKey={["skillentry", "autocomplete", "skill"]}
    />
  );
}

/** Whose skill it is: always the logged-in user for a new one, never editable. */
export function SkillOwnerField({ className }: { className?: string }) {
  const label = useFieldLabels(SKILL_ENTRY_METADATA);
  const form = useEntityEditForm();
  const owner = useStore(
    form.store,
    (s: unknown) => (s as { values: SkillEntryValues }).values.owner
  );
  return (
    <ReadOnlyValue label={label("owner")} className={className}>
      {owner?.displayName}
    </ReadOnlyValue>
  );
}

/** The self-rating, 0 (not rated) to 3 (expert). The keys are spelled out for the i18n export. */
export function SkillRatingField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(SKILL_ENTRY_METADATA);
  return (
    <RatingField
      name="rating"
      label={label("rating")}
      className={className}
      levels={[
        t("plugins.skillmatrix.rating.0"),
        t("plugins.skillmatrix.rating.1"),
        t("plugins.skillmatrix.rating.2"),
        t("plugins.skillmatrix.rating.3"),
      ]}
    />
  );
}

/** The interest, 0 (not rated) to 3 (going crazy). The keys are spelled out for the i18n export. */
export function SkillInterestField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(SKILL_ENTRY_METADATA);
  return (
    <RatingField
      name="interest"
      label={label("interest")}
      className={className}
      levels={[
        t("plugins.skillmatrix.interest.0"),
        t("plugins.skillmatrix.interest.1"),
        t("plugins.skillmatrix.interest.2"),
        t("plugins.skillmatrix.interest.3"),
      ]}
    />
  );
}
