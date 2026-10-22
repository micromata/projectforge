import { CHART_ROLE } from "@/lib/charts/roles";

/** The colour of a bubble without business unit colour: grey, so it doesn't pass for a business unit. */
export const BUBBLE_DEFAULT_COLOR = CHART_ROLE.neutral;

/** The colour of an anonymous bubble without business unit colour. */
export const BUBBLE_OTHERS_DEFAULT_COLOR = CHART_ROLE.neutral;

/** The fill opacity of the own bubbles and of the paler anonymous ones. */
export const BUBBLE_OPACITY = { own: 0.55, others: 0.2 } as const;
