import { HugeiconsIcon } from "@hugeicons/react";
import { StarIcon } from "@hugeicons/core-free-icons";
import type { CellRenderProps } from "./cell-types";

/** Skill ratings run 0..3 (SkillEntryDO.MAX_VAL_RATING), never higher. */
const MAX_STARS = 3;

/**
 * N filled stars for a rating of N. Zero renders an em dash rather than an empty
 * cell, so a rated and an unrated row are distinguishable at a glance.
 */
export function RatingCell({ value, t }: CellRenderProps) {
  return <RatingStars value={value} label={t("rating")} />;
}

/**
 * The stars of [RatingCell] on their own, for a list declaring its column by hand (the skill matrix).
 * [label] names the rating for screen readers ("Self-rating: 2").
 */
export function RatingStars({
  value,
  label,
}: {
  value: unknown;
  label: string;
}) {
  const rating = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(rating) || rating <= 0) {
    return <span className="text-muted-foreground">—</span>;
  }
  const stars = Math.min(Math.round(rating), MAX_STARS);
  return (
    <span
      className="inline-flex items-center gap-0.5 text-brand-yellow"
      role="img"
      aria-label={`${label}: ${stars}`}
    >
      {Array.from({ length: stars }, (_, index) => (
        <HugeiconsIcon
          key={index}
          icon={StarIcon}
          size={13}
          fill="currentColor"
        />
      ))}
    </span>
  );
}
