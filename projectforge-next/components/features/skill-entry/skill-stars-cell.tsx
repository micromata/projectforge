"use client";

import { RatingStars } from "@/components/data-table/cells/rating-cell";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { SKILL_ENTRY_METADATA } from "@/lib/metadata/skill-entry.generated";

/** A rating or interest of the list as stars, named by its column for screen readers. */
export function SkillStarsCell({
  value,
  field,
}: {
  value: number | null | undefined;
  field: "rating" | "interest";
}) {
  const label = useFieldLabels(SKILL_ENTRY_METADATA);
  return <RatingStars value={value} label={label(field)} />;
}
