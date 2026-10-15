/**
 * The traffic light of a contribution margin in %, shared by the order book's contribution margin tab and
 * the editor of its configuration (`ContributionMarginConfig` in the backend): red below `redThreshold`,
 * yellow below `targetPercentage`, green from the target on.
 */
export interface ContributionMarginLimits {
  targetPercentage: number;
  redThreshold: number;
}

export type ContributionMarginTone = "red" | "yellow" | "green";

export function contributionMarginTone(
  percentage: number,
  limits: ContributionMarginLimits
): ContributionMarginTone {
  if (percentage < limits.redThreshold) return "red";
  if (percentage < limits.targetPercentage) return "yellow";
  return "green";
}

/** The background class of the traffic light dot of each tone. */
export const CONTRIBUTION_MARGIN_TONE_BG: Record<
  ContributionMarginTone,
  string
> = {
  red: "bg-destructive",
  yellow: "bg-warning",
  green: "bg-brand-green",
};

/** The text class of each tone, e.g. for a large percentage. */
export const CONTRIBUTION_MARGIN_TONE_TEXT: Record<
  ContributionMarginTone,
  string
> = {
  red: "text-destructive",
  yellow: "text-warning",
  green: "text-brand-green-dark",
};
